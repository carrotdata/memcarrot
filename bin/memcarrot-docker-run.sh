#!/usr/bin/env bash

: '
  This script is used to run the memcarrot docker image.
'

show_help() {
    echo "Usage: memcarrot-docker-run.sh [--image IMAGE] [--tag NAME] [--name CONTAINER] [--help]"
    echo
    echo "   -i, --image docker id      Specify the image ID. Example: f277649686cb08687af03ca784afebc4e0c7a29f889ec438eefaf667f3ee1758"
    echo "   -t, --tag                  Specify the tag of 0.14.2-arm64"
    echo "   -n  --name                 Specify the container name. Example: memcarrot"
    echo "   -h, --help                 Display this help message"
}

# Initialize variables
image=""
tag=""
container_name=""

# Parse short options with getopts
while getopts ":hi:t:n:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        i )
            image=$OPTARG
            ;;
        t )
            tag=$OPTARG
            ;;
        n )
            container_name=$OPTARG
            ;;
        \? )
            echo "Invalid option: $OPTARG" 1>&2
            show_help
            exit 1
            ;;
        : )
            echo "Invalid option: $OPTARG requires an argument" 1>&2
            show_help
            exit 1
            ;;
    esac
done
shift $((OPTIND -1))

# Manually parse long options
while [[ $# -gt 0 ]]; do
    case "$1" in
        --help)
            show_help
            exit 0
            ;;
        --image)
            if [[ -n $2 ]]; then
                image=$2
                shift 2
            else
                echo "Error: --image requires an argument." 1>&2
                show_help
                exit 1
            fi
            ;;
        --tag)
            if [[ -n $2 ]]; then
                tag=$2
                shift 2
            else
                echo "Error: --tag requires an argument." 1>&2
                show_help
                exit 1
            fi
            ;;
        --name)
            if [[ -n $2 ]]; then
                container_name=$2
                shift 2
            else
                echo "Error: --container_name requires an argument." 1>&2
                show_help
                exit 1
            fi
            ;;
        *)
            echo "Invalid option: $1" 1>&2
            show_help
            exit 1
            ;;
    esac
done

if [ -z "$tag" ] && [ -z "$image" ]; then
    echo "Please provide the --image or --tag of carrotdata/memcarrot"
    show_help
    exit 1
fi

image_name="carrotdata/memcarrot"
if [ ! -z "$tag" ]; then
    image_name="${image_name}:${tag}"
else
    image_name="${image}"
fi

docker network create --driver bridge memcarrot_network

docker run --network memcarrot_network -d \
  --name "${container_name}" \
  -p 11211:11211 \
  -e server.port=11211 \
  -e server.address=0.0.0.0 \
  -e save.on.shutdown=true \
  "${image_name}"

sleep 1

docker ps
docker logs "$(docker ps -q)"
docker exec "$(docker ps -q)" cat /users/carrotdata/memcarrot/logs/memcarrot.log
