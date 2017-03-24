@echo off
cd /d %~dp0
set MEMORY=2G
set DATA=data/
set OVTK_DATA=config/
echo Running OVTK with %MEMORY% and data dir %DATA% edit runme.bat to change this amount

setLocal EnableDelayedExpansion
set CLASSPATH="
 for /R ./lib %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 set CLASSPATH=!CLASSPATH!"


java -Xmx%MEMORY% -Dondex.dir=%DATA% -Dovtk.dir=%OVTK_DATA% -Dplugin.scan.lib=false -classpath !classpath! net.sourceforge.ondex.ovtk2.Main %1