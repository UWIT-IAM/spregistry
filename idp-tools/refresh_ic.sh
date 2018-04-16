#!/bin/bash
# get a copy of incommon metadata
# run by cron

root=/data/local/idp-3.3
log=/logs/idp/refresh_ic.log
date >> $log
curl -o ${root}/metadata/InCommon-metadata.xml http://md.incommon.org/InCommon/InCommon-metadata.xml >> $log 2>&1


