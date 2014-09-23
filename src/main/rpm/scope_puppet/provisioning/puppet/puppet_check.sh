#!/bin/sh

if [ ! $# -eq 1 ]
  then
    echo 'Synopsis: $0 <puppet_module_name>'
    exit 1
fi 

echo "Running Puppet with $1" 
puppet apply -e "include $1" --debug --verbose --trace --detailed-exitcodes --logdest=/var/log/puppet/agent.log --autoflush

if [ $? -eq 0  -o $? -eq 2 ]
  then
    echo "************** Puppet run completed SUCCESSFULLY ******************"
    exit 0
  else
    echo "************** Puppet run has FAILURES  ******************"
    exit 1
fi

