@echo off
rem 无论当前盘符是 F 还是 I，先切到脚本所在盘符+目录
cd /d "%~dp0"
gradlew %*