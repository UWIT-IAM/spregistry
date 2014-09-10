#!/bin/bash

# basic installation script

# parse args

function usage {
  echo "usage: $0 [-c local_config] [-v] [-p playbook] [-t] target "
  exit 1
}

# get the base path
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base=${dir%/ansible}

cd $dir

target=
verbose=
playbook=install.yml

# generic parser
prefix=""
key=""
value=""
inventory=""
for keyValue in "$@"
do
  case "${prefix}${keyValue}" in
    -p=*|--playbook=*)  key="-p";     value="${keyValue#*=}";; 
    -t=*|--target=*)  key="-t";     value="${keyValue#*=}";; 
    -i=*|--inventory=*)  key="-i";     value="${keyValue#*=}";; 
    -n*|--no_update)      key="-n";    value="";;
    -v*|--verbose)      key="-v";    value="";;
    -d*|--debug)      key="-d";    value="";;
    *)       value=$keyValue;;
  esac
  case $key in
    -p) playbook=${value}; echo "p=$playbook";  prefix=""; key="";;
    -t) target="${value}";          prefix=""; key="";;
    -i) inventory="${value}";          prefix=""; key="";;
    -v) verbose="-v";           prefix=""; key="";;
    -d) verbose="-vvvv";           prefix=""; key="";;
    -n) TEST=1;           prefix=""; key="";;
    *)   prefix="${keyValue}=";;
  esac
done

target=$target

[[ -n "$target" ]] || usage

# convert the spring properties file to a config yml
properties="${base}/spreg.properties"
config=properties.yml
echo "# don't edit.  This is a binary created by install.sh" > $config
echo "" >> $config
exec 0<$properties
while read line
do
    case $line in
     \#* ) echo "$line" >> $config
           ;;
     *=*) 
       key=${line%=*}
       val=${line##*=}
       fixkey="`echo $key|sed -e 's/\./_/g'`"
       fixval="`echo $val | sed -e '
         s/\&/\\\&/g
         s/\;/\\\;/g
         s/\${\([a-z]*\)\.\([a-z]*\)}/\${\1_\2}/g
       '`"
       # write the yml config
       echo "$fixkey: `eval echo $fixval`" >> $config
       # set the key=value
       eval "`eval echo ${fixkey}`=\"${fixval}\""
       ;;
    esac
done


# make sure the war file was generated
[[ -f ${base}/target/spreg.war ]] || {
   echo "use 'mvn package' to make the war file first"
   exit 1
}

# check ansible vars
[[ -z $iam_ansible ]] && {
   echo "iam.ansible property is missing from spreg.properties"
   exit 1
}

[[ -L tasks ]] || {
  echo "creating tasks link"
  ln -s ${iam_ansible}/tasks .
}

# assemble vars

vars="target=${target} "

ansible-playbook ${playbook} $verbose  -i ${iam_ansible}/hosts  --extra-vars "${vars}"

