#!/bin/bash

# basic installation script

# parse args

function usage {
  echo "usage: $0 [-c local_config] [-v] [-p playbook] [-t] target "
  exit 1
}

config="../spreg.yml"
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
    -c=*|--config_filename=*)  key="-c";     value="${keyValue#*=}";; 
    -p=*|--playbook=*)  key="-p";     value="${keyValue#*=}";; 
    -t=*|--target=*)  key="-t";     value="${keyValue#*=}";; 
    -i=*|--inventory=*)  key="-i";     value="${keyValue#*=}";; 
    -n*|--no_update)      key="-n";    value="";;
    -v*|--verbose)      key="-v";    value="";;
    *)       value=$keyValue;;
  esac
  case $key in
    -c) config=${value};  prefix=""; key="";;
    -p) playbook=${value}; echo "p=$playbook";  prefix=""; key="";;
    -t) target="${value}";          prefix=""; key="";;
    -i) inventory="${value}";          prefix=""; key="";;
    -v) verbose="-v";           prefix=""; key="";;
    -n) TEST=1;           prefix=""; key="";;
    *)   prefix="${keyValue}=";;
  esac
done

target=$target

# scan the config file
[[ -f "$config" ]] || {
  echo "$config not found"
  exit 1
}
exec 4<$config
while read -u4 yline
do
   eval `echo "$yline"| sed -e 's/: */=/'`
done

[[ -n "$target" ]] || usage
[[ -z $inventory ]] && inventory=${iam_ansible}/hosts

[[ -f ../target/spreg.war ]] || {
   echo "use 'mvn package' to make the war file first"
   exit 1
}

# assemble vars

vars="target=${target} config=${config}"

ansible-playbook ${playbook} $verbose -i $inventory  --extra-vars "${vars}"

