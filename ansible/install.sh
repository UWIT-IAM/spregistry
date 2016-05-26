#!/bin/bash

# spregistry ansible installation script

function usage {
  echo "usage: $0  [-v] [-l limit_host] target "
  echo "       $0 -H  target (shows hostnames)"
  echo "       [-v]           ( verbose )"
  echo "       targets: tools_eval | tools_prod"
  exit 1
}

# get the base path
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base=${dir%/ansible}

cd $dir

playbook="install-app.yml"
list_hosts=0
verb=0
debug=0
target=
limit=
gettools=1

# limited args to playbook
OPTIND=1
while getopts 'h?l:Hvdp:' opt; do
  case "$opt" in
    h) usage
       ;;
    \?) usage
       ;;
    l) limit=$OPTARG
       ;;
    p) playbook=$OPTARG
       ;;
    H) listhosts=1
       ;;
    v) verb=1
       ;;
    d) debug=1
       ;;
    q) gettools=0
       ;;
  esac
done

eval target="\${$OPTIND}"
[[ -z $target ]] && usage

# get ansible-tools

[[ -d ansible-tools ]] || {
   echo "installing ansible-tools tools"
   git clone ssh://git@git.s.uw.edu/iam/ansible-tools.git
   gettools=0
} || {
(( getools>0 )) && {
      cd ansible-tools
      git pull origin master
      cd ..
   }
}

export ANSIBLE_LIBRARY=ansible-tools/modules:/usr/share/ansible

((listhosts>0)) && {
   ansible-playbook ${playbook} --list-hosts -i ansible-tools/hosts  --extra-vars "target=${target}"
   exit 0
}


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
   mod="`find ../src -newer ../target/spreg.war|egrep -v '\.js$|\.css$'`"
   [[ -n $mod ]] && {
      echo "spreg war file appears out of date"
      echo "use 'mvn clean package' to update the war file first"
      exit 1
   }
}


# run the installer 

vars=
(( verb>0 )) && vars="$vars -v "
(( debug>0 )) && vars="$vars -vvvv "
[[ -n $limit ]] && vars="$vars -l $limit "
ansible-playbook ${playbook} $vars -i ansible-tools/hosts  --extra-vars "target=${target}"


