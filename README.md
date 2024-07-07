# Memcarrot
Memcarrot: A caching server fully compatible with memcached protocol, offering superior memory utilization (memory overhead is as low as 8 bytes per object), real-time data compression for keys and values, efficient handling of expired items, zero internal and external memory fragmentation, intelligent data tiering and complete persistence support.

# Compilation of source code
*Compilation*
````
    mvn clean install
````

*Create docker image*
```dockerfile
   docker build -t memcarrot:latest .
```
or you can use sh from [bin_docker_runtime](bin_docker_runtime)
```dockerfile
   ./docler_build.sh
```

*run image in docker container without docker-compose.yml*
```dockerfile
docker run -d \
  --name memcarrotapp \
  -v $(pwd)/../conf:/users/apps/carrotdata/memcarrot/conf \
  -e MEMCARROT_APPS_PARAMS="/users/apps/carrotdata/memcarrot/conf/memcarrot.cfg" \
  -e MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=/users/apps/carrotdata/memcarrot/conf/log4j2.xml" \
  memcarrot:latest
```
or execute
```dockerfile
bin_docker_runtime/docker_run.sh
```

Application should be up and ready to go

*You can connect to your docker bash working directory by uses following command* 
```dockerfile
docker exec -it memcarrotapp bash
```
or execute
```dockerfile
[docker_memcarrot_console.sh](bin_docker_runtime%2Fdocker_memcarrot_console.sh)
```