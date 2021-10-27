FROM ubuntu:20.04

LABEL maintainer="Berend Weel <b.weel@esciencecenter.nl>"

RUN apt-get update && \
	apt-get install -y --no-install-recommends build-essential openjdk-11-jdk python3 python3-dev python3-pip && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ENV TZ=Europe/Amsterdam
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt-get update && \
    apt-get install -y apt-transport-https ca-certificates curl software-properties-common && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - && \
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable" && \
    apt-get install -y docker-ce-cli

RUN mkdir /home/xenon
RUN pip3 install --upgrade pip && \
    pip3 install setuptools && \
    pip3 install cwltool

RUN cwltool --version

COPY ./build/distributions/xenonflow-v1.0.1.tar /app/

WORKDIR /app
RUN tar -xf xenonflow-v1.0.1.tar

EXPOSE 9050

CMD ./bin/xenonflow
