#!/usr/bin/env bash

# Determine the directory of the script
SCRIPT_DIR=$(dirname "$(realpath "$0")")

# Set the application directory to the parent directory of the script
APP_DIR=$(dirname "$SCRIPT_DIR")

echo "Memcarrot server home directory is ${APP_DIR}"

cd "${APP_DIR}" || exit

source ./bin/setenv.sh

if [ -z "${JAVA_HOME}" ]; then
  echo "Please set the environment variables JAVA_HOME"
  exit 1
fi

#===== set JVM options =====
export JVM_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/java.security=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-opens java.base/sun.security.action=ALL-UNNAMED --add-opens jdk.naming.rmi/com.sun.jndi.rmi.registry=ALL-UNNAMED \
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.net=ALL-UNNAMED \
--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
${MEMCARROT_APP_OPTS}"

#===== find pid =====
pid() {
  echo "$(ps -aef | grep "${MEMCARROT_INSTANCE_NAME}" | grep -v grep | awk {'print $2'})"
}

#===== start Memcarrot =====
start() {
  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    echo "Server address and port number in use PID=${PID}"
    exit 1
  fi

  if [ "${JMX_EXPORTER_ENABLED}" == "true" ]; then
    echo "JMX exporter enabled"
  else
    JMX_EXPORTER=""
    echo "JMX exporter disabled"
  fi

  exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} ${JMX_EXPORTER} -jar ${MEMCARROT_RELEASE} ${MEMCARROT_APPS_PARAMS} start"
  echo "${exec_cmd}"
  mkdir -p logs
  nohup ${exec_cmd} &
  echo "Memcarrot instance is starting, please wait..."

  sleep 1

  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    echo "Memcarrot instance successfully started. PID ${PID}"
    exit 0
  fi

  echo "Memcarrot instance failed to start, see log file for details"
  exit 1
}

#==== stop Memcarrot ====
stop() {
  PID=$(pid)
  if [ ! -z "${PID}" ]; then
    exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} -jar ${MEMCARROT_RELEASE} ${MEMCARROT_APPS_PARAMS} stop"
    nohup ${exec_cmd} >> /dev/null &
    echo "Memcarrot instance is terminating on PID ${PID}, please wait..."

    # Wait for the process to exit
    while [ -n "$(pid)" ]; do
      sleep 1
    done

    echo "Memcarrot instance has exited."
  else
    echo "No Memcarrot instance is running."
  fi
}

#======= restart Memcarrot =====
restart() {
    echo "Restarting the service..."
    stop
    start
}

# Add the call to start or stop functions based on arguments
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    restart
    ;;
  *)
    echo "Usage: $0 {start|stop|restart}"
    exit 1
    ;;
esac

