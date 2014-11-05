#*

*#

## scans rp-filter for:

##   - new tgtid db entries   -> updates tgtid database
##   - gws activators         -> new gws-activators.xml file
##   - nameid exceptions      -> new nameid-filter.xml


from optparse import OptionParser
import simplejson as json
import syslog
log=syslog.syslog
log_debug=syslog.LOG_DEBUG
log_info=syslog.LOG_INFO
log_err=syslog.LOG_ERR
log_alert=syslog.LOG_ALERT


import xml.etree.ElementTree as ET

import psycopg2 as dbapi2

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
   doc = ET.parse(config['gws_activators'])
   acts = doc.getroot().findall('ActivationRequirement')
   for act in acts:
      # print 'adding existing gws for ' + act.get('entityId')
      gwsEntities.add(act.get('entityId'))

## load the existing nameid exceptions
## these have specific structure: AttributeFilterPolicy -> PolicyRequirementRule(and) -> Rule(not) -> Rule(or) -> rules

def getNameidEntities():
   doc = ET.parse(config['nameid_filter'])
   afps = doc.getroot().findall('{urn:mace:shibboleth:2.0:afp}AttributeFilterPolicy')
   for afp in afps:
      if afp.get('id') == 'releaseTransientIdToAnyone':
         prr = afp.find('{urn:mace:shibboleth:2.0:afp}PolicyRequirementRule')
         rnot = prr.find('{urn:mace:shibboleth:2.0:afp:mf:basic}Rule')
         ror = rnot.find('{urn:mace:shibboleth:2.0:afp:mf:basic}Rule')
         for rule in ror:
            if rule.get('value'):
               # print('loading nameid exception for ' + rule.get('value'))
               nameidEntities.add(rule.get('value'))
         

 
def parseFilter(file):

   global db

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
            # print 'adding ' + eid + ' for ' +  id
            nameidEntities.add(eid)
         if id=='gws_groups' or id=='entitlement_gartner': 
            if eid not in gwsEntities:
               print 'adding ' + eid + ' for ' +  id
               gwsEntities.add(eid)
            else: print 'gws entry already there for ' + eid

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
logname = 'idp_conf_update'
if 'log_name' in config: logname = config['log_name']

log_facility = syslog.LOG_SYSLOG
if 'syslog_facility' in config:
   logf = config['syslog_facility']
   if re.match(r'LOG_LOCAL[0-7]', logf): log_facility = eval('syslog.'+logf)

option = syslog.LOG_PID
if options.verbose: option |= syslog.LOG_PERROR
syslog.openlog(logname, option, log_facility)
log(log_info, "starting.  (conf='%s')" % (options.config))

openDb()
getGwsActivators()
getNameidEntities()

parseFilter(config['rp_filter_file'])

# write out the gws activator



