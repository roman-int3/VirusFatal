#!/bin/bash
# chkconfig: 345 20 80
# description: VirusFatal ScanBalancer server.
#

# Get function from functions library
. /etc/init.d/functions

# Source networking configuration.
. /etc/sysconfig/network

# Check that networking is up.
[ "$NETWORKING" = "no" ] && exit 0

# Set this to your Java installation
if [ -z "$JAVA_HOME" ]; then                               # if JAVA_HOME is undefined
   if [ -f /usr/share/java-utils/java-functions ]; then
      . /usr/share/java-utils/java-functions; set_jvm      # JPackage standard method to set JAVA_HOME
    elif [ -n "$(type -t setJava)" ]; then
      . setJava ""; fi                                     # old SUSE method to set JAVA_HOME
   if [ -z "$JAVA_HOME" ]; then echo "Unable to set JAVA_HOME environment variable"; exit 1; fi
fi

scriptFile=$(readlink -fn $(type -p $0))                   # the absolute, dereferenced path of this script file
scriptDir=$(dirname $scriptFile)                           # absolute path of the script directory
applDir="$scriptDir"                                       # home directory of the service application
serviceName="scanbalancer"                                     # service name
serviceUser="scanbalancer"                                     # OS user name for the service
serviceUserHome="$applDir"                                 # home directory of the service user
serviceGroup="scanbalancer"                                    # OS group name for the service
serviceLogFile="$scriptDir/logs/$serviceName.log"               # log file for StdOut/StdErr
maxShutdownTime=15                                         # maximum number of seconds to wait for the daemon to terminate normally
pidFile="$scriptDir/$serviceName.pid"                      # name of PID file (PID = process ID number)
javaCommand="java"                                         # name of the Java launcher without the path
javaExe="$JAVA_HOME/bin/$javaCommand"                      # file name of the Java application launcher executable
javaCmdOptions="-Xmn100M -Xms200M -Xmx200M"
javaArgs="-jar $javaCmdOptions scanBalancer.jar"                               # arguments for Java launcher
javaCommandLine="$javaExe $javaArgs"                       # command line to start the Java service application
javaCommandLineKeyword="scanBalancer.jar"                          # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others
etcInitDFile="/etc/init.d/$serviceName"                  # symlink to this script from /etc/init.d



# Makes the file $1 writable by the group $serviceGroup.
function makeFileWritable {
   local filename="$1"
   touch $filename || return 1
   chgrp $serviceGroup $filename || return 1
   chmod 775 $filename || return 1

   return 0; 
}

# Returns 0 if the process with PID $1 is running.
function checkProcessIsRunning {
   local pid="$1"
   if [ -z "$pid" -o "$pid" == " " ]; then return 1; fi
   if [ ! -e /proc/$pid ]; then return 1; fi
   return 0; 
}

# Returns 0 if the process with PID $1 is our Java service process.
function checkProcessIsOurService {
   local pid="$1"
   if [ "$(ps -p $pid --no-headers -o comm)" != "$javaCommand" ]; then return 1; fi
   grep -q --binary -F "$javaCommandLineKeyword" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then return 1; fi
   return 0; 
}

# Returns 0 when the service is running and sets the variable $pid to the PID.
function getServicePID {
   if [ ! -f $pidFile ]; then return 1; fi
   pid="$(<$pidFile)"
	
	echo "check PID: "$pid
   checkProcessIsRunning $pid || return 1
   checkProcessIsOurService $pid || return 1
   return 0; 
}

function startServiceProcess {
   cd $applDir || return 1
   rm -f $pidFile
   makeFileWritable $pidFile || return 1
   makeFileWritable $serviceLogFile || return 1
   cmd="nohup $javaCommandLine >>$serviceLogFile 2>&1 & echo \$! >$pidFile"
   su -m $serviceUser -s $SHELL -c "$cmd" || return 1

   sleep 0.1
   pid="$(<$pidFile)"
   if checkProcessIsRunning $pid; then :; else
      echo -ne "\n$serviceName start failed, see logfile.\n"
      return 1
   fi
   return 0; 
}

function stopServiceProcess {
   kill $pid || return 1
   for ((i=0; i<maxShutdownTime*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo -e "\n$serviceName did not terminate within $maxShutdownTime seconds, sending SIGKILL..."
   kill -s KILL $pid || return 1
   local killWaitTime=15
   for ((i=0; i<killWaitTime*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo "Error: $serviceName could not be stopped within $maxShutdownTime+$killWaitTime seconds!"
   return 1; 
}

function startService {
   getServicePID
   if [ $? -eq 0 ]; then echo -n "$serviceName is already running"; return 0; 
   fi
   echo -n "Starting $serviceName   "
   startServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; 
   fi
   echo "started PID=$pid"

   return 0; 
}

function stopService {
   getServicePID
   if [ $? -ne 0 ]; then echo -n "$serviceName is not running"; echo ""; return 0; 
   fi
   echo -n "Stopping $serviceName   "
   stopServiceProcess
   if [ $? -ne 0 ]; then echo "failed"; return 1; 
   fi
   echo "stopped PID=$pid"
 
   return 0; 
}

function checkServiceStatus 
{
   echo -n "Checking for $serviceName:   "
   if getServicePID; then
        echo "running PID=$pid"
   else
        echo "stopped"
        return 3
   fi
   return 0; 
}


function installService
{
        getent group $serviceGroup >/dev/null 2>&1
        if [ $? -ne 0 ]; then
                echo Creating group $serviceGroup
                groupadd -r $serviceGroup || return 1
        fi
        id -u $serviceUser >/dev/null 2>&1
        if [ $? -ne 0 ]; then
                echo Creating user $serviceUser  #-d $serviceUserHome -s$SHELL-d/var/empty -s/sbin/nologin
                #useradd -r -c"$serviceName service" -g root -G root -d $serviceUserHome $serviceUser
		useradd -r -c"$serviceName service" -g $serviceGroup -G users -d $serviceUserHome $serviceUser
        fi

        ln -s $scriptFile $etcInitDFile || return 1
	chown $serviceUser:$serviceUser -R $scriptDir
        chkconfig $serviceName --add
        chkconfig $serviceName  on
        echo $serviceName installed.

        return 0;
}

function uninstallService
{
	stopService
        chkconfig $serviceName off
        chkconfig --del $serviceName
	userdel $serviceName
        rm -f $etcInitDFile
        echo $serviceName uninstalled.
        return 0;
}




# See how we were called.
   RETVAL=0
case "$1" in
      start)                                               # starts the Java program as a Linux service
        startService
	RETVAL=$?	
         ;;
      stop)                                                # stops the Java program service
        stopService
	RETVAL=$?
         ;;
      restart)                                             # stops and restarts the service
        stopService
	startService
        RETVAL=$?
	 ;;
      status)                                              # displays the service status
        checkServiceStatus
        RETVAL=$? 
	;;
      install)                                             # installs the service in the OS
        installService
        RETVAL=$?
	 ;;
      uninstall)                                           # uninstalls the service in the OS
	uninstallService
        RETVAL=$?
	 ;;
*)
         echo "Usage: $0 {install|uninstall|start|stop|restart|status}"
         exit 1
         
esac

exit $RETVAL
