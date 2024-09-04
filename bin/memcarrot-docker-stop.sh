
: '
  This script is used to stop the memcarrot docker image.
'

show_help() {
    echo "Usage: memcarrot-docker-stop.sh[--name NAME] [--help]"
    echo
    echo "   -n, --image name Specify the container name 'carrotdata/memcarrot'"
    echo "   -h, --help       Display this help message"
}

# Initialize variables
image_name=""

# Parse short options with getopts
while getopts ":h:n:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        n )
            image_name=$OPTARG
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
        --iname_name)
            if [[ -n $2 ]]; then
                image_name=$2
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

if [ -z "$image_name" ]; then
    echo "Please provide the --image_name of memcarrot"
    show_help
    exit 1
fi

docker exec ${image_name} /users/carrotdata/memcarrot/bin/memcarrot.sh stop
