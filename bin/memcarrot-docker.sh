#!/usr/bin/env bash

#===== start Memcarrot in docker container =====
start() {
  username="${GITHUB_USERNAME}"
  token="${GITHUB_TOKEN}"
  if [ -z "${username}" ] || [ -z "${token}" ];
  then
    if [ -z "$1" ] || [ -z "$2" ];
    then
      echo "Github user name and token are required"
      echo "Please enter your Github username and token in interactive mode..."
    else
      username="$1"
      token="$2"
    fi
  fi

  docker login ghcr.io -u "${username}" --password-stdin <<< "${token}"

  docker pull ghcr.io/carrotdata/memcarrot:latest

  docker run -d \
    -p 11211:11211 \
    -e server.address=0.0.0.0 \
    ghcr.io/carrotdata/memcarrot:latest

  docker ps
  docker logs -f "$(docker ps -q)"
  docker logout ghcr.io
}

#==== stop Memcarrot in docker container ====
stop() {
  container="$1"
  if [ -z "$container" ];
  then
    echo "Pass parameter #1 as container name"
    exit
  fi
  docker stop "${container}"
  docker ps
  docker images
  echo
  echo Is carrotdata image still in the list?
  echo
  docker images | grep -i carrotdata
}

# Add the call to start or stop functions based on arguments
case "$1" in
  start)
    start "$2" "$3"
    ;;
  stop)
    stop $2
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    echo "  when $0 start 'github user name' 'github token'"
    echo "  when $0 stop 'docker's container name'"
    exit 1
    ;;
esac