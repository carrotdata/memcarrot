#!/usr/bin/env bash

# Yeah, let set individual JAVA_HOME in .bashrc ?
# JAVA_HOME variable could be set on stand alone not-dev server.
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home
export MEMCARROT_VERSION="memcarrot-0.14"
export MAX_HEAP_SIZE=200m
export MEMCARROT_RELEASE=lib/${MEMCARROT_VERSION}-bundle.jar
export MEMCARROT_APPS_PARAMS="./conf/memcarrot.cfg"
export MEMCARROT_INSTANCE_NAME=I_$(pwd)
export MEMCARROT_APP_OPTS="-DMEMCARROT_VERSION=${MEMCARROT_VERSION} -DSTATS_TASK -DSTATS_TASK_INTERVAL=30 -Dlocation=${MEMCARROT_INSTANCE_NAME} -Dlog4j2.configurationFile=./conf/log4j2.xml"
