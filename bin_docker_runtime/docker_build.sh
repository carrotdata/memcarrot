#!/usr/bin/env bash

cd ..
docker_imagetag=latest
docker build -t memcarrot:${docker_imagetag} .