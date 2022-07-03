# syntax=docker/dockerfile:1
FROM openjdk:18-jdk-slim-buster
SHELL ["/bin/bash", "-c"]
WORKDIR /home

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        python3.7 \
        python3-pip \
        python3.7-dev \
        python3-setuptools \
        python3-wheel \
        gcc && \
        pip3 install rdbtools python-lzf && \
        # apt-g`et install openjdk-18-jre -y && \
        # apt-get install openjdk-18-jdk -y && \
        mkdir /home/.sessionFiles
COPY ./redis-rdb-compare/fast-parse.py /home/.
COPY ./redis-rdb-compare/target/redis-rdb-compare-1.0-SNAPSHOT.jar /home/.
# COPY ./redis-rdb-compare/src/main/resources/application.properties /home/.


ENV SLACK_BOT_TOKEN ${SLACK_BOT_TOKEN}
ENV SLACK_APP_TOKEN ${SLACK_APP_TOKEN}
ENV SLACK_SIGNING_SECRET ${SLACK_SIGNING_SECRET}

CMD ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "-cp", "redis-rdb-compare-1.0-SNAPSHOT.jar", "org.example.Main" ]
