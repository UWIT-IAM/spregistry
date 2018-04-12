#  ========================================================================
#  Copyright (c) 2014 The University of Washington
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ========================================================================
#

# scans rp-filter for:

#   - new tgtid db entries   -> updates local tgtid database (psql)
#   - gws activators         -> new gws-activators.xml file
#   - nameid specifications  -> conf/saml-nameid-groups.xml


import os
import time
import string
import shutil
import copy
import re

from subprocess import Popen
from subprocess import PIPE

from lxml import etree
import psycopg2 as dbapi2
import smtplib
from email.mime.text import MIMEText

from optparse import OptionParser
import json
import jinja2

import syslog
log = syslog.syslog
log_debug = syslog.LOG_DEBUG
log_info = syslog.LOG_INFO
log_err = syslog.LOG_ERR
log_alert = syslog.LOG_ALERT

j2_env = None

db = None
config = None

warning_msg = '<!-- DON\'T EDIT!  This file created by, and overwritten by, filter_scan.py -->'

def _print(str):
    pass

# some namespaces we need
ns = {
   'beans': 'http://www.springframework.org/schema/beans',
   'afp': 'urn:mace:shibboleth:2.0:afp',
   'resolver': 'urn:mace:shibboleth:2.0:resolver',
}


# eptid db
def openDb():
    global db
    db = dbapi2.connect(host=config.tgtid_db['db_host'], database=config.tgtid_db['db_name'],
                        user=config.tgtid_db['db_user'], password=config.tgtid_db['db_pass'])


# load the existing attribute resolver activators

# type = label
# values = list of sp entity ids
# beanid: = config bean id
# refid = attribute ref triggering activation
# attrids = list of triggering attributes
resolverEntities = [
    {'type': 'gws', 'values': set([]), 'beanid': 'uw.GetsGwsMemberships', 'refid': 'gws_groups', 'attrids': ['gws_groups']},
    {'type': 'courses', 'values': set([]), 'beanid': 'uw.GetsCourseMemberships', 'refid': 'sln_courses', 'attrids': ['sln_courses']},
]
resolverNeedsUpdate = False

# type = label
# values = list of sp entity ids
# beanid: = config bean id
# attrid = triggering attribute id
nameidEntities = [
    {'type': 'eppn', 'values': set([]), 'beanid': 'uw.GetsEppnNameId', 'attrid': 'eppnNameId'},
    {'type': 'netid', 'values': set([]), 'beanid': 'uw.GetsNetidNameId', 'attrid': 'idNameId'},
    {'type': 'tgtid', 'values': set([]), 'beanid': 'uw.GetsTgtidNameId', 'attrid': 'nameIDPersistentID'},   # sb: persistentIdNameId
    {'type': 'email', 'values': set([]), 'beanid': 'uw.GetsEmailNameId', 'attrid': 'uwEduEmailNameId'},
]
nameidNeedsUpdate = False

tgtidEntity = {'values': set([]), 'refid': 'tgtid2', 'attrids': []}


def _get_entity_values(entities, file):
    doc = etree.parse(file)
    for entity in entities:
        bean = doc.getroot().find('.//*[@id="%s"]' % entity['beanid'])
        for value in bean.findall('.//beans:value', ns):
            _print('found existing %s: %s' % (entity['type'], value.text))
            entity['values'].add(value.text)
        entity['valuesX'] = copy.deepcopy(entity['values'])


# get existing entities from bean definition files
def get_existing_entities():
    global resolverEntities
    global nameidEntities

    _get_entity_values(resolverEntities, config.conf_dir + config.attribute_resolver_activators)
    _get_entity_values(nameidEntities, config.conf_dir + config.nameid_exceptions)


# get attributes that need activation
def get_gws_attributes():
    doc = etree.parse('../conf/attribute-resolver.xml')
    for ent in resolverEntities:
        for attr in doc.getroot().findall('.//resolver:AttributeDefinition/resolver:Dependency[@ref="%s"]' % (ent['refid']), ns):
            _print('adding %s to %s' % (attr.getparent().get('id'), ent['type']))
            ent['attrids'].append(attr.getparent().get('id'))
    for attr in doc.getroot().findall('.//resolver:AttributeDefinition/resolver:Dependency[@ref="%s"]' % (tgtidEntity['refid']), ns):
        _print('adding %s to tgtid triggers' % (attr.getparent().get('id')))
        tgtidEntity['attrids'].append(attr.getparent().get('id'))


# parse a filter file, collect special-needs sp entityids

def parseFilter(file):

    global db
    global resolverEntities
    global resolverNeedsUpdate
    global nameidEntities
    global nameidNeedsUpdate
    global tgtidEntity

    doc = etree.parse(file)

    afps = doc.getroot()
    for afp in afps:

        prr = afp.find('afp:PolicyRequirementRule', ns)

        # entity id
        eid = prr.get('value')

        # scan release rules
        for ar in afp.findall('afp:AttributeRule', ns):
            aid = ar.get('attributeID')
            # nameid specials.  release of the 'refid' means gets the nameid
            for entity in nameidEntities:
                if aid == entity['attrid']:
                    if eid not in entity['values']:
                        _print('adding %s exception for %s ' % (entity['type'], eid))
                        entity['values'].add(eid)
                        nameidNeedsUpdate = True
                    else:
                        entity['valuesX'].discard(eid)

            # resolver activations. release of the attribute implies activation
            for entity in resolverEntities:
                if aid in entity['attrids']:
                    if eid not in entity['values']:
                        # print 'adding %s trigger for %s' % (entity['type'], eid)
                        entity['values'].add(eid)
                        resolverNeedsUpdate = True
                    else:
                        entity['valuesX'].discard(eid)

            # tgtids, release of the attribute requires database entry
            if aid in tgtidEntity['attrids']:
                # print eid + ' used tgtid'
                c1 = db.cursor()
                c1.execute("SELECT rpno FROM rp where rpid='%s';" % (eid))
                row = c1.fetchone()
                c1.close()
                if row is None:
                    _print('adding tgtid entry for ' + eid)
                    c1 = db.cursor()
                    c1.execute("insert into rp values ( (select max(rpno) from rp)+1, '%s');" % (eid))
                    c1.close()
                    db.commit()

    # remove any entities no longer special
    for ent in nameidEntities:
        for v in ent['valuesX']:
            _print('removing exception for ' + v)
            nameidNeedsUpdate = True
            ent['values'].remove(v)
    for ent in resolverEntities:
        for v in ent['valuesX']:
            resolverNeedsUpdate = True
            ent['values'].remove(v)


# verify a saml xml file.  uses external java app (from shib)
def verifySaml(prog, file):
    proc = Popen([prog, '--inFile', file,
                  '--validateSchema'
                  '--schemaExtension', '/schema/shibboleth-2.0-afp-mf-basic.xsd',
                  '--schemaExtension', '/schema/shibboleth-2.0-afp-mf-saml.xsd'], shell=False,
                 stdout=PIPE, stderr=PIPE)

    (out, err) = proc.communicate()

    proc.wait()
    if proc.returncode != 0:
        log(log_err, 'saml document %s is not valid' % (file))
        return False
    return True


# output a bean def file from a template, save the previous
def output_bean_def(dest_file, tmpl_file, ents):
    ents['warning'] = warning_msg
    dest = config.conf_dir + dest_file
    _print('dest=' + dest)
    template = j2_env.get_template(tmpl_file)
    tout = config.tmp_dir + dest_file
    _print('tout=' + tout)
    with open(tout, 'w') as f:
        f.write(template.render(info=ents))
    arc = config.archive_dir + dest_file + '.' + time.strftime('%d')
    shutil.copy2(dest, arc)
    os.rename(tout, dest)
    log(log_info, "Created new " + dest_file)


def sendNotice(sub):
    msg = MIMEText(sub)
    msg['Subject'] = sub
    msg['From'] = config['mail_from']
    msg['To'] = config['mail_to']
    s = smtplib.SMTP(config['mail_smtp'])
    s.sendmail(msg['From'], [msg['To']], msg.as_string())
    s.quit


# ---------
#
# Main
#
# ----------

parser = OptionParser()
parser.add_option('-v', '--verbose', action='store_true', dest='verbose', help='?')
parser.add_option('-c', '--conf', action='store', type='string', dest='config', help='config file')
options, args = parser.parse_args()

# import config
config_source = 'spreg_conf'
if options.config is not None:
    config_source = options.config
    _print('using config=' + config_source)
config = __import__(config_source)

# setup logging
logname = 'idp_filter_scan'
if hasattr(config, 'log_name'):
    logname = config.log_name
log_facility = syslog.LOG_SYSLOG
if hasattr(config, 'syslog_facility'):
    logf = config.syslog_facility
    if re.match(r'LOG_LOCAL[0-7]', logf):
        log_facility = eval('syslog.'+logf)
option = syslog.LOG_PID
if options.verbose:
    option |= syslog.LOG_PERROR

    def _print(str):
        print (str)
syslog.openlog(logname, option, log_facility)
log(log_info, "Filterscan starting.")


#
# open the tgtid database
#
openDb()

#
# gather activator and exception info
#
get_gws_attributes()
get_existing_entities()
parseFilter(config.conf_dir + config.rp_filter_file)

j2_env = jinja2.Environment(trim_blocks=True, lstrip_blocks=True, loader=jinja2.FileSystemLoader(config.template_dir))

#
# update activators and exceptions as needed
#
if resolverNeedsUpdate:
    ents = {}
    for e in resolverEntities:
        ents[e['type']] = e['values']
    output_bean_def(config.attribute_resolver_activators, config.attribute_resolver_activators_j2, ents)

if nameidNeedsUpdate:
    ents = {}
    for e in nameidEntities:
        ents[e['type']] = e['values']
    output_bean_def(config.nameid_exceptions, config.nameid_exceptions_j2, ents)

log(log_info, "Filterscan completed.")
