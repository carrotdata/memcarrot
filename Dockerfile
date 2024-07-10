FROM openjdk:11-jre-slim AS builder

LABEL org.opencontainers.image.source = "https://github.com/carrotdata/memcarrot"
LABEL org.opencontainers.image.description = "memcarrot docker image"

WORKDIR /users/apps/carrotdata/memcarrot
# copy start helper scripts
COPY bin_docker_runtime/memcarrot.sh /users/apps/carrotdata/memcarrot
COPY bin_docker_runtime/setenv.sh /users/apps/carrotdata/memcarrot

# copy binary code
COPY ./target/memcarrot-0.11-SNAPSHOT-jar-with-dependencies.jar /users/apps/carrotdata/memcarrot

EXPOSE 11211

ENTRYPOINT ["bash"]
CMD ["./memcarrot.sh"]
