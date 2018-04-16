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

# update the local (UW) metadata and filter resources as needed from the spregistry database

import os
import time
import string
import shutil
import re
import socket
import dashlib

from optparse import OptionParser
import json

from subprocess import Popen
from subprocess import PIPE

import psycopg2 as dbapi2
import datetime
import smtplib
from email.mime.text import MIMEText


# syslog shortcuts
import syslog
log = syslog.syslog
log_debug = syslog.LOG_DEBUG
log_info = syslog.LOG_INFO
log_err = syslog.LOG_ERR
log_alert = syslog.LOG_ALERT


config = None
tmpdir = 'tmp'

needFilterScan = False

db = None


# modify time of a file
def mTime(filename):
    t = os.path.getmtime(filename)
    return datetime.datetime.fromtimestamp(t)


def openDb():
    global db
    db = dbapi2.connect(host=config.spreg_db['db_host'],
                        database=config.spreg_db['db_name'],
                        user=config.spreg_db['db_user'],
                        password=config.spreg_db['db_pass'])


def countNewRows(group):
    global db
    c1 = db.cursor()
    mtime = None

    mtime = mTime(config.idp_base + group['dir'] + '/' + group['filename'])
    c1.execute("select count(*) from %s where status=1 and group_id='%s' and update_time > '%s';" % (group['type'], group['id'], mtime))
    row = c1.fetchone()
    c1.close()
    # print 'num new = ', row[0]
    return row[0]


def copyRow(table, row, id, f):
    global db
    c1 = db.cursor()
    c1.execute("select %s from %s where status=1 and id='%s'" % (row, table, id))
    rows = c1.fetchall()
    for row in rows:
        f.write(row[0] + '\n')
    return


def copyData(table, id, f):
    # print 'table=%s, id=%s, f=%s' % (table, id, f)
    global db
    num = 0
    c1 = db.cursor()
    c1.execute("select xml, entity_id from %s where status=1 and group_id='%s'" % (table, id))
    rows = c1.fetchall()
    for row in rows:
        f.write(row[0] + '\n')
        num += 1
    return num


# verify a saml xml file
def verifySaml(prog, file):
    proc = Popen([prog, '--inFile', file,
                  '--validateSchema',
                  '--schemaExtension', '/schema/shibboleth-2.0-afp-mf-basic.xsd',
                  '--schemaExtension', '/schema/shibboleth-2.0-afp-mf-saml.xsd'], shell=False,
                 stdout=PIPE, stderr=PIPE)

    (out, err) = proc.communicate()

    proc.wait()
    if proc.returncode != 0:
        log(log_err, 'saml document %s is not valid' % (file))
        log(log_err, out)
        return False
    return True


# update a conf file
def updateIdpConfig(group):
    id = group['id']
    filename = group['filename']
    file_path = config.idp_base + '/' + group['dir'] + '/' + filename
    tmp_path = config.tmp_dir + filename + '.' + str(os.getpid())

    f = open(tmp_path, 'w')
    copyRow(group['type'] + '_group', 'header_xml', id, f)
    num_row = copyData(group['type'], id, f)
    copyRow(group['type'] + '_group', 'footer_xml', id, f)
    f.close()

    # verify the new file

    if num_row < group['min_rows']:
        log(log_err, '%s document %s is too short: %d<%d' % (group['type'], tmp_path, num_row, group['min_rows']))
        return False

    if not verifySaml(config.saml_sign, tmp_path):
        return False

    # is ok, replace original

    sav = config.archive_dir + filename + time.strftime('%d%H%M%S')
    shutil.copy2(file_path, sav)
    os.rename(tmp_path, file_path)

    log(log_info, "Created new %s file %s" % (group['type'], filename))
    needFilterScan = True
    return True


def findGroup(type, id):
    for g in config.idp_conf_files['groups']:
        if g['type'] == type and g['id'] == id:
            return g
    return None


def sendNotice(sub):
    msg = MIMEText(sub)
    msg['Subject'] = sub
    msg['From'] = config.emails['mail_from']
    msg['To'] = config.emails['mail_to']
    s = smtplib.SMTP(config.emails['mail_smtp'])
    s.sendmail(msg['From'], [msg['To']], msg.as_string())
    s.quit


def updateFiles(group):
    ret = updateIdpConfig(group)
    if not ret:
        print 'update files error: ' + group['type']
        print group
        dashlib.send_alert(config.dash_host, config.dash_port, socket.getfqdn(), "idp-metadata", "1", "metadata update error")
        sendNotice('idp file %s is not valid!' % group['filename'])

# ---------
#
# Main
#
# ---------

parser = OptionParser()
parser.add_option('-v', '--verbose', action='store_true', dest='verbose', help='?')
parser.add_option('-c', '--conf', action='store', type='string', dest='config', help='config file')
parser.add_option('-f', '--force', action='store_true', dest='force', help='force update')
parser.add_option('-t', '--type', action='store', type='string', dest='type', help='type to update (filter|metadata)')
parser.add_option('-g', '--group', action='store', type='string', dest='group', help='group to update')
options, args = parser.parse_args()
config_source = 'spreg_conf'
if options.config is not None:
    config_source = options.config
    print 'using config=' + config_source
config = __import__(config_source)

# setup logging
logname = 'spreg_update'
if hasattr(config, 'log_name'):
    logname = config.log_name

log_facility = syslog.LOG_SYSLOG
if hasattr(config, 'syslog_facility'):
    logf = config. syslog_facility
    if re.match(r'LOG_LOCAL[0-7]', logf):
        log_facility = eval('syslog.'+logf)

option = syslog.LOG_PID
if options.verbose:
    option |= syslog.LOG_PERROR
syslog.openlog(logname, option, log_facility)
log(log_info, "starting.  (conf='%s')" % (config_source))

openDb()

if options.group is not None or options.type is not None:
    log(log_info, "just for'%s' in '%s'" % (options.group, options.type))
    group = findGroup(options.type, options.group)
    if group is not None:
        if options.force or countNewRows(group) > 0:
            updateFiles(group)
        else:
            log(log_info, 'no changes')
    else:
        print options.group + ' not found'

else:

    for group in config.idp_conf_files['groups']:
        log(log_info, "checking '%s %s'" % (group['type'], group['id']))
        if options.force or countNewRows(group) > 0:
            updateFiles(group)

    # send keepalive every time script is run--dash alert will happen
    # if script doesn't complete for 195 minutes
    dashlib.send_keepalive(config.dash_host, config.dash_port, socket.getfqdn(), 
        "idp-metadata", "1","195","metadata load not seen for 3 hours","Upd")

log(log_info, "completed.")
