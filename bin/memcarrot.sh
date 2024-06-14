#!/usr/bin/env bash

START_HOME=$PWD
# within instruction $(dirname "$(realpath "$0")") you can start server from any directory. No just from bin
# for example, you can start app by root from cron. like: /bin/carrot/bin/carrot-server.sh reboot
# it is important if you use auto-start script in case of server reboot.
#START_HOME=$(dirname "$(realpath "$0")")
echo Memcarrot server home directory is "${START_HOME}"

cd "${START_HOME}" || exit

. ./setenv.sh

CPATH="${START_HOME}/../conf:${START_HOME}/../lib/${MEMCARROT_RELEASE}"

export JVM_OPTS="-Xmx${MAX_HEAP_SIZE} --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
	--add-opens java.base/java.security=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED \
	--add-opens java.base/sun.security.action=ALL-UNNAMED --add-opens jdk.naming.rmi/com.sun.jndi.rmi.registry=ALL-UNNAMED \
	--add-opens java.base/sun.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED -cp .:${CPATH} ${MEMCARROT_APP_OPTS}"

#===== find pid =====
pid() {
  echo "$(ps -aef | grep "${MEMCARROT_INSTANCE_NAME}" | grep -v grep | awk {'print $2'})"
}

#===== start Memcarrot =====
start() {
  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    echo Server address and port number in use PID="${PID}"
    exit 1
  fi

  exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} com.carrotdata.memcarrot.Memcarrot ${MEMCARROT_APPS_PARAMS} start"
  echo "${exec_cmd}"
  mkdir -p logs
  nohup ${exec_cmd} >>logs/memcarrot-stdout.log &
  echo "Memcarrot ${MEMCARROT_VERSION} instance is staring on PID ${PID}, please wait..."

  sleep 1

  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    echo "Memcarrot ${MEMCARROT_VERSION} instance successfully started. PID ${PID}"
    exit 0
  fi

  echo "Memcarrot instance failed to start"
  exit 1
}

#==== stop Memcarrot ====
stop() {
  if_continue=$1

  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} com.carrotdata.memcarrot.Memcarrot ${MEMCARROT_APPS_PARAMS} stop"   
    #nohup ${exec_cmd} &
    ${exec_cmd}	
    #echo "Memcarrot instance is terminating on PID ${PID}, please wait..."
    #sleep 5
  fi

#  sleep 1

#  PID=$(pid)
#  if [ ! -z "${PID}" ]; then
#    echo "Memcarrot server still running and can't be stopped for some reason. PID ${PID}"
#  else
#    echo "No instances of Memcarrot server are runnning"
#  fi

#  if [ -z "${if_continue}" ]; then
#    exit 0
#  fi
}

#===== reboot =====
reboot() {
  stop 1
  sleep 2
  start
}

#===== usage =====
usage() {
  echo
  echo Usage:
  echo \$\> \./memcarrot.sh [start]\|[stop]
  echo
}

#==== main =====
${JAVA_HOME}/bin/java -version
cmd=$1
if [ "${cmd}" == "start" ]; then
  start
elif [ "${cmd}" == "stop" ]; then
  stop
#elif [ "${cmd}" == "reboot" ]; then
#  reboot
else
  usage
fi
