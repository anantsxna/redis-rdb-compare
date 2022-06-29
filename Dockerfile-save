# syntax=docker/dockerfile:1
FROM ubuntu:22.04 as redis-build
SHELL ["/bin/bash", "-c"]
WORKDIR /home

RUN apt-get update && \
# apt-get install build-essential -y && \
apt-get -y install pypy3 && \
apt-get -y install python3-pip && \
pip3 install virtualenv && \
virtualenv my-venv && \
my-venv/bin/python3.10 -m pip install --upgrade pip && \
my-venv/bin/pip3.10 install rdbtools python-lzf && \
apt-get install openjdk-18-jre -y && \
# apt-get install openjdk-18-jdk -y && \
mkdir /home/.sessionFiles
COPY ./redis-rdb-compare/fast-parse.py /home/.
COPY ./redis-rdb-compare/target/redis-rdb-compare-1.0-SNAPSHOT.jar /home/.

ENV SLACK_BOT_TOKEN ${SLACK_BOT_TOKEN}
ENV SLACK_APP_TOKEN ${SLACK_APP_TOKEN}
ENV SLACK_SIGNING_SECRET ${SLACK_SIGNING_SECRET}

CMD ["java", "-cp", "redis-rdb-compare-1.0-SNAPSHOT.jar", "org.example.Main" ]
