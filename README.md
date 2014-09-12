# SP Registry


## Configure

Create spreg.properties.dev and spreg.properties.prod  from spreg.properties.tmpl

```
## Build

$> mvn clean compile package
```

```
## Install

$> cd ansible

see the README for configuration steps

$> ./install.sh -h (target)
```

```
## IdP install

$> cd idp-tools

see the README for configuration steps
$> ./install.sh -h (target)
```

