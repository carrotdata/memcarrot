
export MEMCARROT_APP_OPTS="-Xmx200m -XX:MaxDirectMemorySize=256m -DSTATS_TASK -DSTATS_TASK_INTERVAL=30 \
-Dlog4j2.configurationFile=/users/carrotdata/memcarrot/conf/log4j2.xml"
export JMX_EXPORTER_ENABLED=false
export JMX_EXPORTER="-javaagent:/users/carrotdata/memcarrot/lib/jmx_prometheus_javaagent-1.0.1.jar=9191:/users/carrotdata/memcarrot/conf/jmxconfig.yml"
