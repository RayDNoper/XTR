#!/bin/sh

KEY_PASS=${KEY_PASS:-defaultpassword}

keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -keystore /app/ssl/keystore.p12 \
        -dname "CN=, OU=, O=, L=, ST=, C=" -storepass "$KEY_PASS" -validity 3650

java -cp app:app/lib/* ee.buerokratt.xtr.XTRApplication
