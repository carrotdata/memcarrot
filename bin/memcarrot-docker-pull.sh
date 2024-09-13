#!/usr/bin/env bash

: '
  This script is used to pull a docker image from github.
'

show_help() {
    echo "Usage: memcarrot-docker-pull.sh [--tag TAG] [--help]"
    echo
    echo "  -t, --tag TAG Specify the version of memcarrot"
}

# Parse short options with getopts
while getopts ":h:t:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        t )
            tag=$OPTARG
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
        --tag)
            if [[ -n $2 ]]; then
                tag=$2
                shift 2
            else
                echo "Error: --version requires an argument." 1>&2
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

if [ -z "$tag" ]; then
    echo "Please provide the --tag of memcarrot"
    show_help
    exit 1
fi

docker_image="carrotdata/memcarrot"

docker pull docker.io/${docker_image}:${tag}
