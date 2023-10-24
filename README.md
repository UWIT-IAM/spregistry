# SP Registry

Provides a web application that allows University of Washington SP operators to submit thier SP's metadata to the University's IdP.

Operation is 90% automatic.  The only manual intervention is release of attributes.

The application is quite specific to resources at the University, such as a DNS ownership API, that may not be available to other institutions.


## Configure

Create spreg.properties.dev and spreg.properties.prod  from spreg.properties.tmpl

## Dependencies

See README.dependencies for information on a special step needed to prepare dependencies for the build.

## Build

```
$> mvn clean compile package
```

## Install

```
$> cd ansible

# see the README for configuration steps

$> ./install.sh -h (target)
```

## IdP install

```
$> cd idp-tools

see the README for configuration steps
$> ./install.sh -h (target)
```
