#!/usr/bin/env bash

# Determine the directory of the script
APP_DIR=$(dirname "$(realpath "$0")")

cd "${APP_DIR}" || exit
MEMCARROT_VERSION=0.10
MEMCARROT_RELEASE=memcarrot-${MEMCARROT_VERSION}-all.jar
CPATH="${APP_DIR}/conf:${APP_DIR}/lib/${MEMCARROT_RELEASE}:./target/test-classes:${HOME}/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar"

export JVM_OPTS="-Xmx1g --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
        --add-opens java.base/java.security=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED \
        --add-opens java.base/sun.security.action=ALL-UNNAMED --add-opens jdk.naming.rmi/com.sun.jndi.rmi.registry=ALL-UNNAMED \
        --add-opens java.base/sun.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED -cp .:${CPATH} ${MEMCARROT_APP_OPTS}"

  exec_cmd="${JAVA_HOME}/bin/java ${JVM_OPTS} -Dhost=127.0.0.1 -Dport=11211 com.carrotdata.memcarrot.TestSimpleClient"
  echo "${exec_cmd}"
  ${exec_cmd}
