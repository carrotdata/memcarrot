FROM openjdk:17-jdk-slim as builder

WORKDIR /users/apps/carrotdata/memcarrot

COPY bin_docker_runtime /users/apps/carrotdata/memcarrot
COPY ./target/memcarrot-0.11-SNAPSHOT.jar /users/apps/carrotdata/memcarrot

RUN ls -l /users/apps/carrotdata/memcarrot

EXPOSE 8084

ENTRYPOINT ["bash"]
CMD ["./memcarrot.sh"]
