@echo off

set jarPath="%~dp0\loamen_spider.jar"
set md5Path="%jarPath%.md5"


if exist %jarPath% (
    certUtil -hashfile "%jarPath%" MD5 | find /i /v "md5" | find /i /v "certutil" > "%md5Path%"
)

set jarPath="%~dp0\tvbox.jar"
set md5Path="%jarPath%.md5"


if exist %jarPath% (
    certUtil -hashfile "%jarPath%" MD5 | find /i /v "md5" | find /i /v "certutil" > "%md5Path%"
)