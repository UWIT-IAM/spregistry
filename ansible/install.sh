#!/bin/bash

# spregistry ansible installation script

function usage {
  echo "
usage:  $0 [options] target
options:        [-v]            ( verbose )
                [-d]            ( very verbose )
                [-l hostname]   ( limit install to 'hostname' )
                [-H]            ( list hosts in the cluster )

target help:    $0 targets
  "
  exit 1
}

function targets {
  echo "
     eval                       Installs to eval hosts (iamtools-test11)
     prod                       Installs to prod hosts (iamtools21, 22)
  "
  exit 1
}

# get the base path
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base=${dir%/ansible}
cd $dir

playbook="install-app.yml"
listhosts=0
verb=0
debug=0
target=
limit=

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
  esac
done

shift $((OPTIND-1))
target="$1"
[[ "$target" == "eval" || "$target" == "prod" || "$target" == "targets" ]] || usage
[[ "$target" != "targets" ]] || targets
[[ -z $playbook ]] && playbook="install-app.yml"
echo "Installing $playbook to $target"
[[ -r $playbook ]] || {
  echo "Playbook $playbook not found!"
  exit 1
}

((listhosts>0)) && {
   ansible-playbook ${playbook} --list-hosts -i ./hosts  --extra-vars "target=${target}"
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
[[ -n $limit ]] && {
  [[ $limit == *"."* ]] || limit=${limit}.s.uw.edu
  vars="$vars -l $limit "
}
ansible-playbook ${playbook} $vars -i ./hosts  --extra-vars "target=${target}"


