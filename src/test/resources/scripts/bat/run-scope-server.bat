@echo OFF

set CP=.;..\etc
for %%f in (..\lib\*.jar ..\lib\*.so) do call ..\utils\setcp.bat %%f

set CP

set PATH=.;..\lib;%PATH%

java -classpath %CP% com.cisco.oss.foundation.orchestration.main.RunScope