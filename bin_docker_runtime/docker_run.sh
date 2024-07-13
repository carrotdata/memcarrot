#!/usr/bin/env bash

docker run -d \
  --name memcarrotapp \
  -p 11211:11211 \
  -v $(pwd)/../conf:/users/apps/carrotdata/memcarrot/conf \
  -e MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=/users/apps/carrotdata/memcarrot/conf/log4j2.xml" \
  -e save.on.shutdown=true \
  -e server.address=0.0.0.0 \
  memcarrot:latest

#  ghcr.io/carrotdata/memcarrot:latest
