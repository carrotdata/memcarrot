#!/usr/bin/env bash

# Yeah, let set individual JAVA_HOME in .bashrc ?
# JAVA_HOME variable could be set on stand alone not-dev server.
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home
export MEMCARROT_VERSION=0.11
export MEMCARROT_RELEASE=memcarrot-0.11-SNAPSHOT.jar:memcarrot-0.11-SNAPSHOT-jar-with-dependencies.jar
export MEMCARROT_APPS_PARAMS="./conf/memcarrot.cfg"
export MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=./conf/log4j2.xml"
# Ubuntu jemalloc path
# export LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so
# export MALLOC_CONF=prof:true,lg_prof_interval:30,lg_prof_sample:17
# jeprof --show_bytes --gif /path/to/jvm/bin/java jeprof*.heap > /tmp/app-profiling.gif
# Mac jemalloc path
# export LD_PRELOAD=

CPATH="./conf:${MEMCARROT_RELEASE}"

export JVM_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/java.security=ALL-UNNAMED --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-opens java.base/sun.security.action=ALL-UNNAMED --add-opens jdk.naming.rmi/com.sun.jndi.rmi.registry=ALL-UNNAMED \
--add-opens java.base/sun.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
${MEMCARROT_APP_OPTS} -cp .:${CPATH}"
