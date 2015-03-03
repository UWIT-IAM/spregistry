#!/bin/bash

# keep the spregistry database up-to-date with the filesystem 'database'
# necessary until we switch to the database for production

[[ "$1" = "" || "$1" = "--help" || "$1" = "-?" ]] && {
  echo "usage: $0 [file_name] [password_file]"
  echo "file_name = /data/local/sp-registry/UW-rp-metadata.xml  /data/local/sp-registry/rp-filter.xml  /data/local/sp-registry/sp-secrets"
  exit 1
}
filename="$1"

[[ -n $2 ]] && {
  pwd="`cat $2`"
} || {
  echo -n "password: "
  read -s pwd
  echo
}

group_id=
table=

(( record_count=0 ))

#
# --- filter and metadata functions
#

# get a row from the db
row_value=
function get_row {
  ent=$1
  row_value="`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 -c \"select xml from $table where entity_id='$ent';\"`"
  row_value="`echo $row_value|sed -s 's/^ *//'`"
  # echo "[[$row_value]]"
}

# put a row to the db
function put_row {
  ent=$1
  xml="$2"
  echo "add: $ent"
  resp="`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 \
      -c \"insert into $table values ('$ent', '$xml', '$group_id', 1, now());\"`"
  echo $resp
}


# update a row in the db
function update_row {
  ent=$1
  xml="$2"
  echo "upd: $ent"
  echo "`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 \
      -c \"update $table set xml = '$xml' where entity_id='$ent';\"`"
  echo $resp
}



# arg is first line 
function scan_md {
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
  # echo "processing $table: $id"
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
  ftxt="`echo $txt|sed -e \"s/'/''/g\"`"
  etxt="`echo $txt`"
  get_row $id

  [[ "$etxt" == "$row_value" ]] || {
    echo "match fails for $table: $id"
    # echo "sp: ${etxt} "
    # echo "db: $row_value"
    [[ -z "$row_value" ]] && {
       put_row  $id "$ftxt"
    } || {
       update_row  $id "$ftxt"
    }
  }
  
  (( record_count = record_count + 1 ))
}


#
# --- proxy functions
#

proxy_key=
proxy_secret=

# get a row from the db
function get_proxy_row {
  ent=$1
  pid=$2
  # echo "get proxy: $ent : $pid"
  row_value="`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 -c \"select social_key,social_secret from $table where entity_id='$ent' and social_provider='$pid';\"`"
  read proxy_key proxy_secret <<<$(IFS="|";echo $row_value)
  # echo "key=$proxy_key"
  # echo "sec=$proxy_secret"
}

# put a row to the db
function put_proxy_row {
  ent=$1
  pid="$2"
  key="$3"
  secret="$4"
  echo "add: $ent : $pid"
  resp="`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 \
      -c \"insert into proxy values ('$ent', '$pid', '$key', '$secret', 1, now());\"`"
  echo $resp
}


# update a row in the db
function update_proxy_key {
  ent=$1
  pid="$2"
  key="$3"
  echo "upd: $ent : $pid - key"
  echo "`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 \
      -c \"update proxy set social_key = '$key' where entity_id='$ent' and social_provider='$pid';\"`"
  echo $resp
}


# update a row in the db
function update_proxy_secret {
  ent=$1
  pid="$2"
  secret="$3"
  echo "upd: $ent : $pid - secret"
  echo "`PGPASSWORD=$pwd psql -t -U spreg -d spregistry -h iamdbdev01 \
      -c \"update proxy set social_secret = '$secret' where entity_id='$ent' and social_provider='$pid';\"`"
  echo $resp
}


function scan_proxy {
  line="$1"
  l1=${line#\"}
  id=${l1%\"*}
  # echo "proxy: $id"
  while read line
  do
    [[ $line = "}," ]] && break
    l1="${line#\"}"
    pid=${l1%%\"*}
    line="${line#*key\": \"}"
    key="${line%%\"*}"
    line="${line#*secret\": \"}"
    secret="${line%%\"*}"

    get_proxy_row $id $pid

    [[ $key == $proxy_key && $secret == $proxy_secret ]] || {
       echo "match fails for $id : $pid"
       [[ -z "$proxy_key" ]] && {
          put_proxy_row  $id $pid ${key} ${secret}
       } || {
          [[ $key == $proxy_key ]] || update_proxy_key  $id $pid ${key} 
          [[ $secret == $proxy_secret ]] || update_proxy_secret  $id $pid ${secret} 
       }
    }
    (( record_count = record_count + 1 ))
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
    \"*{)   scan_proxy $line
         ;;

    *\<EntityDescriptor*)  scan_md "$line"
         ;;
    *\<AttributeFilterPolicy*) 
         scan_md "$line"
         ;;
  esac
done
echo "$record_count entries scanned"

