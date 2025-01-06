#!/bin/sh

KEY_PASS=${KEY_PASS:-defaultpassword}

openssl pkcs12 -export -out /app/ssl/keystore.p12 -inkey /app/ssl/cert.key -in /app/ssl/cert.crt -name tomcat -passout "pass:$KEY_PASS"

java -cp app:app/lib/* ee.buerokratt.xtr.XTRApplication
