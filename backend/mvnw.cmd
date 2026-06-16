@echo off
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.10\a38810a491b03367137adfdfbe7d14c4"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven not found at %MAVEN_HOME%
    exit /b 1
)
"%MAVEN_HOME%\bin\mvn.cmd" %*
