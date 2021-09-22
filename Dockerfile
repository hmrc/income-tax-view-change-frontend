FROM openjdk:8
ENV SBT_VERSION 1.4.9
RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v1.4.9/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops
# WORKDIR /HelloWorld
# ADD . /HelloWorld
COPY . /src/
WORKDIR /src

CMD /ops/sbt/bin/sbt compile -Dsbt.rootdir=true