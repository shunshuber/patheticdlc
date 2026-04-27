@echo off
cd /d %~dp0
call gradlew.bat clean build
pause
