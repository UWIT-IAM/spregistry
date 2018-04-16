#!/bin/bash

# update uw rp filter and metadata from database

[[ "$1" == "force" ]] || {
   [[ -f /www/refresh_uw/data/refresh ]] || exit 0
   lockfile -r 0 -l 600 /www/refresh_uw/lock || exit 1
}

date

# requires flag file present
rm -f /www/refresh_uw/data/refresh

base=/data/local/idp-3.3/
cd ${base}/local-bin

. py-env/bin/activate
export JAVA_HOME=`java -XshowSettings:properties -version 2>&1  |grep java.home | awk '{print  $3}'`

{

md_pre="`date +%s -r ${base}/metadata/UW-rp-metadata.xml`"
filter_pre="`date +%s -r ${base}/conf/rp-filter.xml`"
nameid_pre="`date +%s -r ${base}/conf/saml-nameid-exceptions.xml`"
attrs_pre="`date +%s -r ${base}/conf/attribute-resolver-activators.xml`"

python spreg_update.py

# if rp-metadata changed, notify idp
md_post="`date +%s -r ${base}/metadata/UW-rp-metadata.xml`"
(( md_post > md_pre + 60 )) && {
   echo "notify idp of metadata change"
   ${base}/bin/reload-service.sh -id shibboleth.MetadataResolverService
}

filter_post="`date +%s -r ${base}/conf/rp-filter.xml`"

# if rp-filter changed, run the filter analyzer
(( filter_post > filter_pre + 60 )) && {
   echo "running the filter analyzer"
   python filter_scan.py
}

nameid_post="`date +%s -r ${base}/conf/saml-nameid-exceptions.xml`"
attrs_post="`date +%s -r ${base}/conf/attribute-resolver-activators.xml`"
 
(( nameid_post > nameid_pre + 60 )) && {
   echo "notify idp of nameid exceptions change"
   ${base}/bin/reload-service.sh -id shibboleth.NameIdentifierGenerationService
}

(( attrs_post > attrs_pre + 60 )) && {
   echo "notify idp of attribute resolver activators change"
   ${base}/bin/reload-service.sh -id shibboleth.AttributeResolverService
}

(( filter_post > filter_pre + 60 )) && {
   echo "notify idp of attribute filter change"
   ${base}/bin/reload-service.sh -id shibboleth.AttributeFilterService
}

rm -f /www/refresh_uw/lock

} >> /logs/idp/refresh_uw.log


