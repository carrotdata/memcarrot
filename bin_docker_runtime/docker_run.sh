#!/usr/bin/env bash

#  -p 11211:11211 \f
docker run -d \
  --name memcarrotapp \
  -v $(pwd)/../conf:/users/apps/carrotdata/memcarrot/conf \
  -e MEMCARROT_APPS_PARAMS="/users/apps/carrotdata/memcarrot/conf/memcarrot.cfg" \
  -e MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=/users/apps/carrotdata/memcarrot/conf/log4j2.xml" \
  memcarrot:latest
