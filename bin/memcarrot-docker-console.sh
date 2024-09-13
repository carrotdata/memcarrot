#!/usr/bin/env bash

: '
  This script is used to run the memcarrot docker image.
'

show_help() {
    echo "Usage: docker_build.sh [--image IMAGE] [--name CONTAINER] [--help]"
    echo
    echo "   -i, --image docker id      Specify the image ID. Example: f277649686cb08687af03ca784afebc4e0c7a29f889ec438eefaf667f3ee1758"
    echo "   -n  --name                 Specify the container name. Example: memcarrot"
    echo "   -h, --help                 Display this help message"
}

# Initialize variables
image=""
container_name=""

# Parse short options with getopts
while getopts ":hi:n:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        i )
            image=$OPTARG
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

if [ -z "$container_name" ] && [ -z "$image" ]; then
    echo "Please provide the --image or --name of carrotdata/memcarrot"
    show_help
    exit 1
fi

image_name=
if [ ! -z "$container_name" ]; then
    image_name="${container_name}"
else
    image_name="${image}"
fi

# Run the memcarrot docker image
docker exec -it ${image_name} sh
