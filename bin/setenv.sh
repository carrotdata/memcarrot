#!/usr/bin/env bash

# Yeah, let set individual JAVA_HOME in .bashrc ?
# JAVA_HOME variable could be set on stand alone not-dev server.
# export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home

export MEMCARROT_RELEASE="lib/memcarrot-0.16.1-bundle.jar"
export MEMCARROT_APPS_PARAMS="./conf/memcarrot.cfg"
export MEMCARROT_INSTANCE_NAME=I_$(pwd)

export MEMCARROT_APP_OPTS="-Xmx200m -XX:MaxDirectMemorySize=256m -DSTATS_TASK -DSTATS_TASK_INTERVAL=30 \
-Dlocation=${MEMCARROT_INSTANCE_NAME} -Dlog4j2.configurationFile=./conf/log4j2.xml"
if [ -z "${JMX_EXPORTER_ENABLED}" ]; then
  export JMX_EXPORTER_ENABLED=false
fi
export JMX_EXPORTER="-javaagent:lib/jmx_prometheus_javaagent-1.0.1.jar=9191:conf/jmxconfig.yml"
