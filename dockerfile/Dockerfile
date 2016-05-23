FROM       centos:centos7
MAINTAINER Takari <tamas@takari.io>

ENV NEXUS_PERF_HOME /opt/sonatype/nexus-perf
ENV NEXUS_PERF_DATA_VERSION 1.0.2
ENV NEXUS_PERF_CLIENT_VERSION 1.0.7
ENV NEXUS_PERF_CLIENT_JMX_HOST 192.168.99.100
ENV NEXUS_PERF_CLIENT_JMX_PORT 5000

ENV JAVA_HOME /opt/java
ENV JAVA_VERSION_MAJOR 8
ENV JAVA_VERSION_MINOR 92
ENV JAVA_VERSION_BUILD 14

RUN yum install -y \
  curl tar \
  && yum clean all

# install Oracle JDK
RUN mkdir -p /opt \
  && curl --fail --silent --location --retry 3 \
  --header "Cookie: oraclelicense=accept-securebackup-cookie; " \
  http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/jdk-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz \
  | gunzip \
  | tar -x -C /opt \
  && ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} ${JAVA_HOME}

RUN mkdir -p ${NEXUS_PERF_HOME} \
  && curl --fail --silent --location --retry 3 --output ${NEXUS_PERF_HOME}/nexus-perf.jar \
    http://repo1.maven.org/maven2/io/takari/nexus/nexus-perf/${NEXUS_PERF_CLIENT_VERSION}/nexus-perf-${NEXUS_PERF_CLIENT_VERSION}-jar-with-dependencies.jar

RUN curl --fail --silent --location --retry 3 \
    https://repo1.maven.org/maven2/io/takari/nexus/perf/data/maven/maven01/${NEXUS_PERF_DATA_VERSION}/maven01-${NEXUS_PERF_DATA_VERSION}-data.tar.gz \
  | gunzip \
  | tar -x -C ${NEXUS_PERF_HOME}

RUN useradd -r -u 200 -m -c "nexus role account" -d ${NEXUS_PERF_HOME} -s /bin/false nexus

EXPOSE ${NEXUS_PERF_CLIENT_JMX_PORT}
VOLUME ${NEXUS_PERF_HOME}
WORKDIR ${NEXUS_PERF_HOME}
USER nexus

ENV MAX_HEAP 768m
ENV MIN_HEAP 256m
ENV JAVA_OPTS -server -Djava.net.preferIPv4Stack=true -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=${NEXUS_PERF_CLIENT_JMX_PORT} -Djava.rmi.server.hostname=${NEXUS_PERF_CLIENT_JMX_HOST}
CMD ${JAVA_HOME}/bin/java ${JAVA_OPTS} -jar nexus-perf.jar remote
