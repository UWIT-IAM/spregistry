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

# update the local (UW) metadata, filter, and proxy resources
# as needed from the spregistry database

import os
import time
import string
import shutil

from optparse import OptionParser
import simplejson as json

from subprocess import Popen
from subprocess import PIPE

# syslog shortcuts
import syslog
log=syslog.syslog
log_debug=syslog.LOG_DEBUG
log_info=syslog.LOG_INFO
log_err=syslog.LOG_ERR
log_alert=syslog.LOG_ALERT

import psycopg2 as dbapi2
import datetime

import smtplib
from email.mime.text import MIMEText


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
  db = dbapi2.connect(host=config['db_host'], database=config['db_name'], user=config['db_user'], password=config['db_pass'])
 
def countNewRows(group):
  global db
  c1 = db.cursor()
  mtime = None
 
  if group['type']=='proxy':
     mtime = mTime(config['proxy_base'] + group['metadata_filename'])
     c1.execute("select count(*) from %s where status=1 and update_time > '%s';" % (group['type'], mtime))
  else:
     mtime = mTime(config['idp_base'] + group['dir'] + '/' + group['filename'])
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
   global db
   num = 0
   c1 = db.cursor()
   c1.execute("select xml from %s where status=1 and group_id='%s'" % (table, id))
   rows = c1.fetchall()
   for row in rows:
      f.write(row[0] + '\n')
      num += 1
   return num
   

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


# update a config file
def updateIdpConfig(group):
   id = group['id']
   filename = group['filename']
   file_path = config['idp_base'] + '/' + group['dir'] + '/' + filename
   tmp_path = config['idp_base'] + '/' + config['tmp_dir'] + '/' + filename + '.' + str(os.getpid())
  
   f = open(tmp_path, 'w')
   copyRow(group['type'] + '_group', 'header_xml', id, f)
   num_row = copyData(group['type'], id, f)
   copyRow(group['type'] + '_group', 'footer_xml', id, f)
   f.close()

   # verify the new file

   if num_row<group['min_rows']:
      log(log_err, '%s document %s is too short: %d<%d' % (group['type'], tmp_path, num_row, group['min_rows']))
      return False
     
   if not verifySaml(config['saml_sign'], tmp_path): return False

   # is ok, replace original
   
   sav = config['idp_base'] + config['archive_dir'] + filename + time.strftime('%d%H%M%S')
   shutil.copy2(file_path, sav)
   os.rename(tmp_path, file_path)
   
   log(log_info, "Created new %s file %s" % (group['type'], filename))
   needFilterScan = True
   return True
   

def findGroup(type, id):
   for g in config['idp_config']['groups']: 
      if g['type']==type and g['id']==id: return g
   for g in config['proxy_config']['groups']: 
      if g['type']==type and g['id']==id: return g
   return None

#
# proxy configs
#

def updateProxyConfig(group):
   global db

   m_name = group['metadata_filename']
   s_name = group['secret_filename']

   # first build the secrets file
   secrets = {}
   num = 0
   c1 = db.cursor()
   c1.execute("select entity_id,social_provider,social_key,social_secret from proxy where status=1")
   rows = c1.fetchall()
   for row in rows:
      eid = row[0]
      sid = row[1]
      # print 'eid: ' + eid + ', sid: ' + sid
      if not eid in secrets: secrets[eid] = {}
      secrets[eid][sid] = {"key": row[2], "secret": row[3]}
      num += 1
   # print '%d social keys found' % num
   
   if num<5:
      log(log_err, '%s document is too short: %d<%d' % (group['type'], num, 5))
      return False
     
   s_file_path = config['proxy_base'] + s_name
   s_tmp_path = config['proxy_base'] + config['tmp_dir'] + s_name + '.' + str(os.getpid())
   f = open(s_tmp_path, 'w')
   f.write(json.dumps(secrets))
   f.close()

   # generate a metadata file for the rps in secrets
   
   m_file_path = config['proxy_base'] + m_name
   m_tmp_path = config['proxy_base'] + config['tmp_dir'] + m_name + '.' + str(os.getpid())

   mf = open(m_tmp_path, 'w')
   copyRow('metadata_group', 'header_xml', 'uwrp', mf)


   ## mf = open(config['incommon_md'], 'r')
   for mdfile in config['metadata_files']:
      f = open(mdfile, 'r')
      copying = False
      for line in f:
         if copying:
            mf.write(line)
            if string.find(line, '</EntityDescriptor')>=0: copying = False
         else:
            p = string.find(line,'entityID="')
            if p>0:
               p += 10
               e = string.find(line,'"',p+1)
               eid = line[p:e]
               # print 'entityid: [%s]' % (line[p:e])
               if eid in secrets:
                  # print 'adding: ' + eid + ' from ' + mdfile
                  secrets[eid]['md'] = 1
                  copying = True
                  mf.write(line)
      f.close()
            
   copyRow('metadata_group', 'footer_xml', 'uwrp', mf)
   mf.close()

   # did we get everyone
   for s in secrets:
      if not 'md' in secrets[s]: log(log_err,  'secrets file missed ' + s)
   
   # verify the new file

   if not verifySaml(config['saml_sign'], m_tmp_path): return False

   # all ok, replace originals
   
   # md
   sav = config['proxy_base'] + config['archive_dir'] + m_name + time.strftime('%d%H%M%S')
   shutil.copy2(m_file_path, sav)
   os.rename(m_tmp_path, m_file_path)

   # secret
   sav = config['proxy_base'] + config['archive_dir'] + s_name + time.strftime('%d%H%M%S')
   shutil.copy2(s_file_path, sav)
   os.rename(s_tmp_path, s_file_path)
   
   log(log_info, "Created new %s %s and %s files" % (group['type'], m_name, s_name ))
   return True
   

def sendNotice(sub):
   msg = MIMEText(sub)
   msg['Subject'] = sub
   msg['From'] = config['mail_from']
   msg['To'] = config['mail_to']
   s = smtplib.SMTP(config['mail_smtp'])
   s.sendmail(msg['From'], [msg['To']], msg.as_string())
   s.quit




def updateFiles(group):
   ret = True
   if group['type']=='proxy': ret = updateProxyConfig(group)
   else: ret = updateIdpConfig(group)
   if not ret:
      sendNotice('idp file %s is not valid!')


#---------
#
# Main
#
#----------

parser = OptionParser()
parser.add_option('-v', '--verbose', action='store_true', dest='verbose', help='?')
parser.add_option('-c', '--conf', action='store', type='string', dest='config', help='config file')
parser.add_option('-f', '--force', action='store_true', dest='force', help='force update')
parser.add_option('-t', '--type', action='store', type='string', dest='type', help='type to update (filter|metadata)')
parser.add_option('-g', '--group', action='store', type='string', dest='group', help='group to update')
options, args = parser.parse_args()
config_file = 'spreg_update.conf'
if options.config!=None:
   config_file = options.config
   print 'using config=' + config_file
f = open(config_file,'r')
config = json.loads(f.read())
f.close()

# setup logging
logname = 'spreg_update'
if 'log_name' in config: logname = config['log_name']

log_facility = syslog.LOG_SYSLOG
if 'syslog_facility' in config:
   logf = config['syslog_facility']
   if re.match(r'LOG_LOCAL[0-7]', logf): log_facility = eval('syslog.'+logf)

option = syslog.LOG_PID
if options.verbose: option |= syslog.LOG_PERROR
syslog.openlog(logname, option, log_facility)
log(log_info, "starting.  (conf='%s')" % (config_file))

# 

openDb()

if options.group != None or options.type!=None: 
   log(log_info, "just for'%s' in '%s'" % (options.group, options.type))
   group = findGroup(options.type, options.group)
   if group != None:
      if options.force or countNewRows(group)>0:  updateFiles(group)
      else: log(log_info, 'no changes')
   else:
      print options.group + ' not found'
  
else:

   for group in config['idp_config']['groups']:
      log(log_info, "checking '%s %s'" % (group['type'], group['id']))
      if options.force or countNewRows(group)>0:  updateFiles(group)

   for group in config['proxy_config']['groups']:
      log(log_info, "checking '%s'" % (group['id']))
      if options.force or countNewRows(group)>0:  updateFiles(group)

log(log_info, "completed.")
exit (0)
