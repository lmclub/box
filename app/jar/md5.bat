@echo off

chcp 65001 >nul
setlocal enabledelayedexpansion

set "baseDir=%~dp0"
set "allMd5Path=%baseDir%all_jars.md5"

rem 清空/创建汇总文件
type nul > "%allMd5Path%"

for %%F in ("%baseDir%*.jar") do (
    set "jarPath=%%~fF"
    set "md5Value="

    echo Processing: !jarPath!

    rem 只匹配整行是 16 进制字符的行（即 MD5 值）
    for /f "delims=" %%H in ('certUtil -hashfile "!jarPath!" MD5 ^| findstr /r /c:"^[0-9a-fA-F][0-9a-fA-F]*$"') do (
        set "md5Value=%%H"
    )

    echo MD5: !md5Value!
    echo !md5Value!  %%~nxF>> "%allMd5Path%"
    echo ----------------------------------------
)

echo.
echo 所有 MD5 已汇总到: %allMd5Path%
endlocal
pause