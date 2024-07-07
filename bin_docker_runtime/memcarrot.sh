#!/usr/bin/env bash

. ./setenv.sh

exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} com.carrotdata.memcarrot.Memcarrot ${MEMCARROT_APPS_PARAMS}"
echo "${exec_cmd}"
# Start the application in the background
${exec_cmd} start &
#nohup ${exec_cmd} &
echo "Memcarrot ${MEMCARROT_VERSION} instance is starting... "

# Capture the process ID (PID) of the application
PID=$!

# Define a function to handle SIGTERM signal
term_handler() {
  echo "Stopping application..."
  if [ ! -z "${PID}" ]; then
     ${exec_cmd} stop &
  fi
  wait "$PID"
  echo "Application stopped."
}

# Trap SIGTERM signal and call term_handler
trap 'term_handler' TERM

# Wait for the application process to exit
wait "$PID"