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

## scans rp-filter for:

##   - new tgtid db entries   -> updates tgtid database
##   - gws activators         -> new gws-activators.xml file
##   - nameid exceptions      -> new nameid-filter.xml


import os
import time
import string
import shutil

from subprocess import Popen
from subprocess import PIPE

from optparse import OptionParser
import simplejson as json
import syslog
log=syslog.syslog
log_debug=syslog.LOG_DEBUG
log_info=syslog.LOG_INFO
log_err=syslog.LOG_ERR
log_alert=syslog.LOG_ALERT


import xml.etree.ElementTree as ET
from xml.parsers.expat import ExpatError

import psycopg2 as dbapi2

import smtplib
from email.mime.text import MIMEText

nameidEntities = set([])
nameidNeedsUpdate = False

gwsEntities = set([])
gwsNeedsUpdate = False

db = None
config = None


## eptid db
def openDb():
  global db
  db = dbapi2.connect(host=config['db_host'], database=config['db_name'], user=config['db_user'], password=config['db_pass'])


## load the existing activators
def getGwsActivators():
   global gwsEntities

   doc = ET.parse(config['conf_dir'] + config['gws_activators'])
   acts = doc.getroot().findall('ActivationRequirement')
   for act in acts:
      # print 'adding existing gws for ' + act.get('entityId')
      gwsEntities.add(act.get('entityId'))


## load the existing nameid exceptions
## <AttributeFilterPolicy> 
##   <PolicyRequirementRule(and)>
##        <Rule(not)> <Rule>
##        <Rule(not)> <Rule>
##        . . .

def getNameidEntities():
   global nameidEntities

   doc = ET.parse(config['conf_dir'] + config['nameid_filter'])
   afps = doc.getroot().findall('{urn:mace:shibboleth:2.0:afp}AttributeFilterPolicy')
   for afp in afps:
      if afp.get('id') == 'releaseTransientIdToAnyone':
         prr = afp.find('{urn:mace:shibboleth:2.0:afp}PolicyRequirementRule')
         for br in prr:
            r = br.find('{urn:mace:shibboleth:2.0:afp:mf:basic}Rule')
            if r.get('value'):
               # print('loading nameid exception for ' + r.get('value'))
               nameidEntities.add(r.get('value'))
         

# parse a filter file, recording interesting info
 
def parseFilter(file):

   global db
   global nameidEntities
   global nameidNeedsUpdate
   global gwsEntities
   global gwsNeedsUpdate

   doc=ET.parse(file)
   
   afps = doc.getroot()
   for afp in afps:
      
      prr = afp.find('{urn:mace:shibboleth:2.0:afp}PolicyRequirementRule')
 
      # entity id
      eid = prr.get('value')
      
      # scan rules
      for ar in afp.findall('{urn:mace:shibboleth:2.0:afp}AttributeRule'):
         id = ar.get('attributeID')
         if id=='idNameId' or id=='nameIDPersistentID' or id=='eppnNameId': 
            if eid not in nameidEntities:
               nameidNeedsUpdate = True
               nameidEntities.add(eid)
         if id=='gws_groups' or id=='entitlement_gartner': 
            if eid not in gwsEntities:
               gwsNeedsUpdate = True
               gwsEntities.add(eid)

         if id=='ePTID' or id=='attributePersistentID' or id=='nameIDPersistentID' or id=='saml2PersistentID': 
            # print eid + ' used tgtid'
            c1 = db.cursor()
            c1.execute("SELECT rpno FROM rp where rpid='%s';" % (eid) )
            row = c1.fetchone()
            c1.close()
            if row==None:
               print 'adding tgtid entry for ' +  eid
               c1 = db.cursor()
               c1.execute("insert into rp values ( (select max(rpno) from rp)+1, '%s');" % (eid))
               c1.close()
               db.commit()

# verify a saml xml file
def verifySaml(prog, file):
   proc = Popen([prog,'--inFile',file, \
     '--validateSchema', \
     '--schemaExtension','/schema/shibboleth-2.0-afp-mf-basic.xsd', \
     '--schemaExtension','/schema/shibboleth-2.0-afp-mf-saml.xsd'], shell=False, \
     stdout=PIPE,stderr=PIPE)

   (out,err) = proc.communicate()
  
   proc.wait()
   if proc.returncode!=0:
      log(log_err, 'saml document %s is not valid' % (file))
      return False
   return True


# copy a template file, substuting 'xml' for 'CONTENT'

def writeFromTemplate(tmpl, tgt, xml):
   ftmpl = open(tmpl, 'r')
   ftgt = open(tgt, 'w')
   for row in ftmpl:
      if string.find(row, 'CONTENT')>=0: ftgt.write(xml)
      else: ftgt.write(row)
   ftmpl.close()
   ftgt.close()



# write a new nameid filter file

def outputNameidFilter():

   # assemble content
   xml = ''
   for e in nameidEntities:
      xml = xml + ('<basic:Rule xsi:type="basic:NOT"><basic:Rule xsi:type="basic:AttributeRequesterString" value="%s" /></basic:Rule>\n' % e )
   
   tmp_file = config['idp_base'] + config['tmp_dir'] + config['nameid_filter'] + '.' + str(os.getpid())
   writeFromTemplate(config['conf_dir'] + config['nameid_filter'] + '.tmpl', tmp_file, xml)
   if not verifySaml(config['saml_sign'], tmp_file): return False

   tgt_file = config['conf_dir'] + config['nameid_filter']

   sav = config['idp_base'] + config['archive_dir'] + config['nameid_filter'] + '.' +  time.strftime('%d')
   shutil.copy2(tgt_file, sav)
   os.rename(tmp_file, tgt_file)
   log(log_info, "Created new nameid filter file")
  

# write a new gws activator file

def outputGwsActivators():

   # assemble content
   xml = ''
   for e in gwsEntities:
      xml = xml + ('<ActivationRequirement entityId="%s" />\n' % e )
   
   tmp_file = config['idp_base'] + config['tmp_dir'] + config['gws_activators'] + '.' + str(os.getpid())
   writeFromTemplate(config['conf_dir'] + config['gws_activators'] + '.tmpl', tmp_file, xml)
   
   try:
      doc = ET.parse(tmp_file)
   except (ExpatError):
      log(log_err, 'gws activator file %s invalid' % (tmp_file))
      return False

   tgt_file = config['conf_dir'] + config['gws_activators']

   sav = config['idp_base'] + config['archive_dir'] + config['gws_activators'] + '.' +  time.strftime('%d')
   shutil.copy2(tgt_file, sav)
   os.rename(tmp_file, tgt_file)
   log(log_info, "Created new gws activators file")
  
def sendNotice(sub):
   msg = MIMEText(sub)
   msg['Subject'] = sub
   msg['From'] = config['mail_from']
   msg['To'] = config['mail_to']
   s = smtplib.SMTP(config['mail_smtp'])
   s.sendmail(msg['From'], [msg['To']], msg.as_string())
   s.quit


#---------
#
# Main
#
#----------

parser = OptionParser()
parser.add_option('-v', '--verbose', action='store_true', dest='verbose', help='?')
parser.add_option('-c', '--conf', action='store', type='string', dest='config', help='config file')
options, args = parser.parse_args()
config_file = 'filter_scan.conf'
if options.config!=None:
   config_file = options.config
   print 'using config=' + config_file
f = open(config_file,'r')
config = json.loads(f.read())
f.close()

# setup logging
logname = 'idp_filter_scan'
if 'log_name' in config: logname = config['log_name']

log_facility = syslog.LOG_SYSLOG
if 'syslog_facility' in config:
   logf = config['syslog_facility']
   if re.match(r'LOG_LOCAL[0-7]', logf): log_facility = eval('syslog.'+logf)

option = syslog.LOG_PID
if options.verbose: option |= syslog.LOG_PERROR
syslog.openlog(logname, option, log_facility)
log(log_info, "starting.  (conf='%s')" % (config_file))

openDb()
getGwsActivators()
getNameidEntities()

parseFilter(config['conf_dir'] + config['rp_filter_file'])

# write out the nameid filter
if nameidNeedsUpdate: 
   if not outputNameidFilter(): sendNotice('idp new nameid file fails!')

# write out the gws activators file
if gwsNeedsUpdate: 
   if not outputGwsActivators(): sendNotice('idp new gws activators file fails!')

log(log_info, "completed.")


