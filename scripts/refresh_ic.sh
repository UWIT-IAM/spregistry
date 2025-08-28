#!/bin/bash
# get a copy of incommon metadata
# run for iamcert by cron

log=/logs/refresh_ic_daily.log
echo "SPReg InCommon metadata refresh starting" > $log
date >> $log

cd /data/local/spreg/metadata
curl --cacert /usr/local/ssl/certs/cacerts.cert -o metadata.tmp https://mdq.incommon.org/entities
# if metadata.tmp is less than 50000 blocks in size, it probably didn't download correctly
(( $? == 0 && `ls -s metadata.tmp | awk '{print $1}'` > 50000)) && mv -f metadata.tmp InCommon-metadata.xml
# if it's still there, its size was <= 50000 blocks
if [ -f metadata.tmp ] || [ ! -f InCommon-metadata.xml ]; then
    echo "Retrieval of InCommon metadata failed, or received metadata seems too small" >> $log
else
    chown iamcert.iam-dev InCommon-metadata.xml
    echo "completed" >> $log
fi

/usr/sbin/sendmail -f "uw_iam_sm_auth_core-team" -t  << END
To: markiel@uw.edu
Subject: InCommon metadata daily refresh
Reply-To: uw_iam_sm_auth_core-team@uw.edu

`cat $log`
END
