#!/bin/bash

JARS=`ls ../lib/*jar`
STARTUP=`ls ../lib/scopeServer*jar`

CP=".:../config:../etc"

for x in $JARS
do
  if [ ${x} != ${STARTUP} ] 
  then
     CP="${CP}:${x}"
   fi
done

CP="${CP}:${STARTUP}"

java -classpath $CP com.cisco.oss.foundation.orchestration.main.RunScope $*

	