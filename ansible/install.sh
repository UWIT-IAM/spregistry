#!/bin/bash

# spregistry ansible installation script

function usage {
  echo "usage: $0 [options] target "
  echo "       [-p playbook]  ( default: install.yml )"
  echo "       [-v]           ( verbose )"
  echo "       [-d]           ( very verbose )"
  echo "       [-i inventory] ( default:  ansible-tools/hosts )"
  echo "       [-q]           ( quick:  do not refresh ansible-toools )"
  echo "       targets: rivera_dev | rivera_prod"
  exit 1
}

# get the base path
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base=${dir%/ansible}

cd $dir

target=
verbose=
quick=
list_opt=
playbook=install.yml
force=""

# generic parser
prefix=""
key=""
value=""
inventory=""
for keyValue in "$@"
do
  case "${prefix}${keyValue}" in
    -p=*|--playbook=*)  key="-p";     value="${keyValue#*=}";; 
    -i=*|--inventory=*)  key="-i";     value="${keyValue#*=}";; 
    -n*|--no_update)      key="-n";    value="";;
    -v*|--verbose)      key="-v";    value="";;
    -d*|--debug)      key="-d";    value="";;
    -q*|--quick)      key="-q";    value="";;
    -f*|--force)      key="-f";    value="";;
    -l*|--list)      key="-l";    value="";;
    -h*|-?|--help)             usage;;
    *)       value=$keyValue;;
  esac
  case $key in
    -p) playbook=${value}; echo "p=$playbook";  prefix=""; key="";;
    -i) inventory="${value}";          prefix=""; key="";;
    -v) verbose="-v";           prefix=""; key="";;
    -d) verbose="-vvvv";           prefix=""; key="";;
    -q) quick=q;           prefix=""; key="";;
    -l) list_opt="--list-hosts";           prefix=""; key="";;
    -f) force="f";           prefix=""; key="";;
    *)  prefix="${keyValue}=";;
  esac
done

[[ -z $target ]] && target=$value
[[ -z "$target"  || "$target" == "-"* ]] && usage

# get ansible-tools

[[ -d ansible-tools ]] || {
   echo "installing ansible-tools tools"
   git clone ssh://git@git.s.uw.edu/iam/ansible-tools.git
} || {
   [[ -z $quick ]] && {
      cd ansible-tools
      git pull origin master
      cd ..
   }
}

export ANSIBLE_LIBRARY=ansible-tools/modules:/usr/share/ansible


# store current status
cat > "install.status" << END
SP Registry project (spreg) install

by: `whoami`
on: `date`

target: $target

branch:
`git branch -v| grep '^\*'`

status:
`git status -uno --porcelain`
END

# make sure the war file was generated
[[ -f ../target/spreg.war ]] || {
   echo "spreg war file not found"
   echo "use 'mvn clean package' to make the war file first"
   exit 1
}

# make sure the war file is up-to-date
[[ -z $force ]] && {
   mod="`find ../src -newer ../target/spreg.war`"
   [[ -n $mod ]] && {
      echo "spreg war file appears out of date"
      echo "use 'mvn clean package' to update the war file first"
      exit 1
   }
}


# run the installer 

vars="target=${target} "
ansible-playbook ${playbook} $verbose  -i ansible-tools/hosts  --extra-vars "${vars}" $list_opt

