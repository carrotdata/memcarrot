#!/usr/bin/env bash

github_user=$1
github_token=$2
docker_imagetag=latest
docker_image=memcarrot:${docker_imagetag}

# Login to github
echo ${github_token} | docker login ghcr.io -u $github_user --password-stdin

# Tag your Docker image
docker tag ${docker_image} ghcr.io/${github_user}/${docker_image}

# Push the image to GitHub Container Registry
docker push ghcr.io/${github_user}/${docker_image}