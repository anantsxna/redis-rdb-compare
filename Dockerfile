# syntax=docker/dockerfile:1
FROM ubuntu:22.04 as ubuntu-build
SHELL ["/bin/bash", "-c"]
WORKDIR /home

RUN apt-get update && apt-get install build-essential -y
RUN apt-get -y install pypy3
RUN apt-get -y install python3-pip

RUN pip3 install virtualenv
RUN virtualenv my-venv
RUN my-venv/bin/python3.10 -m pip install --upgrade pip
RUN my-venv/bin/pip3.10 install rdbtools python-lzf
RUN apt-get install openjdk-18-jre -y && apt-get install openjdk-18-jdk -y

COPY ./redis-rdb-compare/fast-parse.py /home/.
COPY ./redis-rdb-compare/target/redis-rdb-compare-1.0-SNAPSHOT.jar /home/.

RUN chown -R root:root /home && chmod 755 /home
RUN mkdir /home/.sessionFiles && chmod a+w /home/.sessionFiles
ENV SLACK_BOT_TOKEN xoxb-3634587634486-3641251395858-YHygrMQIpqiYC03Oskglplty
ENV SLACK_APP_TOKEN xapp-1-A03JEJLC3PZ-3633472021575-43466fd9bda4633c5ba6061fa2b90974c3a50d3a885b5b66bddc83913e02e586
ENV SLACK_SIGNING_SECRET d15933dbfe0c6def2176cdc7e82ae8b0

CMD ["java", "-cp", "redis-rdb-compare-1.0-SNAPSHOT.jar", "org.example.Main" ]
