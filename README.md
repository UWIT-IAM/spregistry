# SP Registry

Provides a web application that allows University of Washington SP operators to submit thier SP's metadata to the University's IdP.

Operation is 90% automatic.  The only manual intervention is release of attributes.

The application is quite specific to resources at the University, such as a DNS ownership API, that may not be available to other institutions.

SLO: 90% availability. This is a developer-facing tool, and callers are usually willing to wait or
email us if any issues come up.

Links:

* Prod endpoint: <https://iam-tools.u.washington.edu/spreg/>
* Test endpoint: <https://iam-tools-test.u.washington.edu/spreg/>

## Cloning

Use `git clone --recurse-submodules` to clone this repo, because it uses
[submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules).

## Configure

Create spreg.properties.dev and spreg.properties.prod  from spreg.properties.tmpl

## Build

```bash
mvn clean compile package
```

## Install

Get access to SSH into the `iamcert` user with an SSH key, then run:

```bash
cd ansible
# see the README for configuration steps
./install.sh -h (target)
```

## IdP install

Get access to SSH into the `iamcert` user with an SSH key, then run:

```bash
cd idp-tools
# see the README for configuration steps
./install.sh -h (target)
```
