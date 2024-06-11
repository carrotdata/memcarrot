FROM openjdk:17-jdk-slim as builder

WORKDIR /users/apps/carrotdata/memcarrot

COPY bin_docker_runtime /users/apps/carrotdata/memcarrot
COPY conf /users/apps/carrotdata/memcarrot/conf
COPY ./target/*.jar /users/apps/carrotdata/memcarrot

RUN ls -l /users/apps/carrotdata/memcarrot

EXPOSE 11211

ENTRYPOINT ["bash"]
CMD ["./memcarrot.sh"]
