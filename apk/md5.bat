@echo off

set jarPath="%~dp0\lmbox.apk"
set md5Path="%jarPath%.md5"


if exist %jarPath% (
    certUtil -hashfile "%jarPath%" MD5 | find /i /v "md5" | find /i /v "certutil" > "%md5Path%"
)

set jarPath="%~dp0\lmvod.apk"
set md5Path="%jarPath%.md5"


if exist %jarPath% (
    certUtil -hashfile "%jarPath%" MD5 | find /i /v "md5" | find /i /v "certutil" > "%md5Path%"
)