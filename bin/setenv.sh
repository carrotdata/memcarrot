#!/usr/bin/env bash

# Yeah, let set individual JAVA_HOME in .bashrc ?
# JAVA_HOME variable could be set on stand alone not-dev server.
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home
export MAX_HEAP_SIZE=100m
export MEMCARROT_VERSION=0.11
export MEMCARROT_RELEASE=carrotdata-memcached-${MEMCARROT_VERSION}-bundle.jar
export MEMCARROT_INSTANCE_NAME=I_$(pwd)
export MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=./conf/log4j2.xml"
export MEMCARROT_APPS_PARAMS="./conf/memcarrot.cfg"
export MEMCARROT_APP_OPTS="-DSTATS_TASK -DSTATS_TASK_INTERVAL=300 -Dlocation=${MEMCARROT_INSTANCE_NAME} -Dlog4j2.configurationFile=../conf/log4j2.xml"
# Ubuntu jemalloc path
# export LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so
# export MALLOC_CONF=prof:true,lg_prof_interval:30,lg_prof_sample:17
# jeprof --show_bytes --gif /path/to/jvm/bin/java jeprof*.heap > /tmp/app-profiling.gif
# Mac jemalloc path
# export LD_PRELOAD=
