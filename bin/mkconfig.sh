#!/bin/bash

# make spreg config files

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base=${dir%/bin}

# parse the config file.  It is very simple yml

cfg=${base}/spreg.yml
exec 0<$cfg
while read yline
do
   line="`echo "$yline"| sed -e 's/: */=/'`"
   eval $line
done

function escapestring {
  echo $1 | sed -e "
s/\//\\\\\//g"
}

spreg_root="`escapestring $spreg_root`"
crt_file="`escapestring $crt_file`"
key_file="`escapestring $key_file`"
ca_file="`escapestring $ca_file`"
gws_dns_member_base_url="`escapestring $gws_dns_member_base_url`"
gws_base_url="`escapestring $gws_base_url`"


# make sure we got what we need
[[ -z $spreg_root || -z $crypt_key || -z $gws_base_url || -z $gws_dns_member_base_url ]] && {
   echo "Not all vars are defined in the config file (spreg.yml)"
   exit 1
}

function translate {
   sed -e "
s/{{spreg_root}}/${spreg_root}/g 
s/{{crt_file}}/${crt_file}/g 
s/{{key_file}}/${key_file}/g
s/{{ca_file}}/${ca_file}/g 
s/{{db_host}}/${db_host}/g
s/{{db_name}}/${db_name}/g 
s/{{db_user}}/${db_user}/g 
s/{{db_pass}}/${db_pass}/g 
s/{{gws_base_url}}/${gws_base_url}/g 
s/{{gws_dns_member_base_url}}/${gws_dns_member_base_url}/g 
s/{{crypt_key}}/${crypt_key}/g 
" < $1 > $2
}

translate ${base}/src/main/webapp/WEB-INF/applicationContext.xml.tmpl ${base}/src/main/webapp/WEB-INF/applicationContext.xml
translate ${base}/src/main/webapp/WEB-INF/spreg-servlet.xml.tmpl ${base}/src/main/webapp/WEB-INF/spreg-servlet.xml

# setup ansible tasks link

[[ -d ${iam_ansible}/tasks ]] && {
  ln -sf ${iam_ansible}/tasks ${base}/ansible/
} || {
  echo "iam-ansible tasks directory not found!"
  exit 1
} 

exit 0
