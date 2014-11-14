#!/bin/bash

# copy metadata, filter, et al to the database
# cd /data/local/sp-registry

[[ "$1" = "--help" || "$1" = "-?" ]] && {
  echo "usage: $0 [file_name] [password_file]"
  exit 1
}
filename="$1"

pwd="$2"
[[ -z "$pwd" ]] && {
  echo -n "password: "
  read -s pwd
  echo
}

group_id=
table=

tmp="sqltmp"
rm -f $tmp

# arg is first line 
function load_md {
  line="$1"
  txt=''
  case $line in
    \<EntityDescriptor*) 
        l1=${line#*entityID=\"}
        id=${l1%\"*}
        ;;
    \<AttributeFilterPolicy*) 
        l1=${line#*id=\"}
        id=${l1%\"*}
        ;;
    *) echo "unknown file type: $line"
       exit 1
       ;;
  esac
  echo "processing $table: $id"
  txt="${line}"
  while read line
  do 
    txt="${txt}
${line}"
    case $line in
       \<PolicyRequirementRule*AttributeRequesterString*) 
           l1=${line#*value=\"}
           id=${l1%\"*}  # the real entity id
           ;;
       \</EntityDescriptor*) break
            ;;
       \</AttributeFilterPolicy*) break
            ;;
    esac
  done
  ftxt="`echo $txt|sed -e 's/"/""/g'`"
  echo "\"${id}\",\"${ftxt}\",${group_id},1," >> $tmp

}

function load_proxy {
  line="$1"
  l1=${line#\"}
  id=${l1%\"*}
  echo "proxy: $id"
  while read line
  do
    [[ $line = "}," ]] && break
    l1="${line#\"}"
    pid=${l1%%\"*}
    line="${line#*key\": \"}"
    key="${line%%\"*}"
    line="${line#*secret\": \"}"
    secret="${line%%\"*}"
    echo "${id},${pid},${key},${secret},1,"  >> $tmp
  done
}

[[ -n $filename ]] && exec<$filename
while read line
do
  case $line in 
    *\<EntitiesDescriptor*) 
        l1=${line#*Name=\"}
        id=${l1%%\"*}
        case $id in
          urn:washington.edu:rpedit) group_id="uwrp"
                                     table="metadata"
                    ;;
          urn:washington.edu:fox) group_id="uwbase"
                                     table="metadata"
                    ;;
          urn:mace:incommon) group_id="incommon"
                                     table="metadata"
                    ;;
          *)  echo "unrecognized metadata file: $id"
              exit 1
              ;;
        esac
        echo "processing a file of metadata info, group_id=$id"
        ;;
    \<AttributeFilterPolicyGroup*) 
        l1=${line#*id=\"}
        id=${l1%%\"*}
        case $id in
          CoreFilterPolicy) group_id="uwcore"
                                     table="filter"
                    ;;
          RandSFilterPolicy) group_id="uwrs"
                                     table="filter"
                    ;;
          ServerRegPolicy) group_id="uwrp"
                                     table="filter"
                    ;;
          *)  echo "unrecognized filter file"
              exit 1
              ;;
        esac
        echo "processing a file of filter info, group_id=$id"
        ;;
  
    {)   group_id=
         table="proxy"
         echo "processing a file of proxy info"
         ;;
    \"*{)   load_proxy $line
         ;;

    *\<EntityDescriptor*)  load_md "$line"
         ;;
    *\<AttributeFilterPolicy*) 
         load_md "$line"
         ;;
  esac
done

##  PGPASSWORD=$pwd psql -U spreg -d spregistry -h iamdbdev01 -c "\copy $table from $tmp csv"
