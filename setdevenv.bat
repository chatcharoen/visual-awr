@echo off
REM This Batch file is used to set all the environment files necessary to 
REM run the eCorp, by Digital Artifacts, Inc.  This batch file should be
REM used on Win32 platforms only.


set APP_HOME=.
set JAVA_HOME=D:\oracle\jdk1.8.0_71
set JAVA_OPTS=-Xms64M -Xmx256M
set ANT_HOME=D:\DEV\apache-ant-1.9.7
REM ###########################################


set CLASSPATH=

set CLASSPATH=%APP_HOME%\lib\dai.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\daiBeans.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\jcommon-1.0.22.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\jfreechart-1.0.17.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\ojdbc5.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\ObfuscationAnnotation.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\commons-compress-1.8.1.jar;%CLASSPATH%
set CLASSPATH=%APP_HOME%\lib\junit-4.12.jar;%CLASSPATH%

set path=%JAVA_HOME%\bin;%APP_HOME%\lib;%path%

set path=%ANT_HOME%\bin;%PATH%

REM cd %APP_HOME%


echo Completed Environment Setup