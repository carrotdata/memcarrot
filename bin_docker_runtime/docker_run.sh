#!/usr/bin/env bash

docker run -d \
  --name memcarrotapp \
  -p 11211:11211 \
  -v $(pwd)/../conf:/users/apps/carrotdata/memcarrot/conf \
  -e MEMCARROT_APPS_PARAMS="/users/apps/carrotdata/memcarrot/conf/memcarrot.cfg" \
  -e MEMCARROT_APP_OPTS="-Dlog4j2.configurationFile=/users/apps/carrotdata/memcarrot/conf/log4j2.xml" \
  ghcr.io/carrotdata/memcarrot:latest
