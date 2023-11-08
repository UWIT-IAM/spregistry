#!/bin/bash

# spregistry ansible installation script

function usage {
  echo "
usage:  $0 [options] product target
options:        [-v]            ( verbose )
                [-d]            ( very verbose )
                [-l hostname]   ( limit install to 'hostname' )
                [-H]            ( list hosts in the cluster )

product help:   $0 products

target help:    $0 targets
  "
  exit 1
}

function products {
  echo "
     app                        Installs the entire SPRegistry product
     war                        Installs just the SPRegistry warfile
     attributes                 Installs new attribute.xml
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

playbook=
target=
limit=
verb=0
debug=0
listhosts=0

# limited args to playbook
while getopts 'h?l:Hvdp:' opt; do
  case "$opt" in
    h) usage
       ;;
    \?) usage
       ;;
    l) limit=$OPTARG
       ;;
    H) listhosts=1
       ;;
    v) verb=1
       ;;
    d) debug=1
       ;;
    p) playbook=$OPTARG
       ;;
    y) force=1
       ;;
  esac
done

shift $((OPTIND-1))
product="$1"
[[ "$product" == "products" ]] && products
target="$2"
[[ "$target" == "eval" || "$target" == "prod" || "$target" == "targets" ]] || usage
[[ "$target" != "targets" ]] || targets
[[ -z $playbook ]] && playbook="install-${product}.yml"

if [[ $target =~ "prod" && force -eq 0 ]]
then
   target=
   read -p "Are you sure you want to push prod? [yN] " -r -n1
   echo
   if [[ $REPLY =~ ^[Yy]$ ]]
   then
      target="prod"
   else
      echo "Aborting"
      exit 1
   fi
fi

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

# make sure the war file is up-to-date, if installing the app
# Maybe we should have a 'force' option
# [[ -z $force || "$product" == "app" ]] && {
[[ "$product" == "app" || "$target" == "war" ]] && {
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
