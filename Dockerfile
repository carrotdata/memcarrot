FROM openjdk:17-jdk-slim as builder

WORKDIR /users/apps/carrotdata/memcarrot

# copy start helper scripts
COPY bin_docker_runtime /users/apps/carrotdata/memcarrot

# copy binary code
COPY ./target/*.jar /users/apps/carrotdata/memcarrot

EXPOSE 11211

ENTRYPOINT ["bash"]
CMD ["./memcarrot.sh"]
