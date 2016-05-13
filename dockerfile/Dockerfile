FROM       centos:centos7
MAINTAINER Takari <tamas@takari.io>

ENV NEXUS_PERF_HOME /opt/sonatype/nexus-perf
ENV NEXUS_PERF_DATA_SCENARIO maven01
ENV NEXUS_PERF_DATA_VERSION 1.0.1
ENV NEXUS_PERF_CLIENT_VERSION 1.0.4

ENV JAVA_HOME /opt/java
ENV JAVA_VERSION_MAJOR 8
ENV JAVA_VERSION_MINOR 74
ENV JAVA_VERSION_BUILD 02

RUN yum install -y \
  curl tar \
  && yum clean all

# install Oracle JRE
RUN mkdir -p /opt \
  && curl --fail --silent --location --retry 3 \
  --header "Cookie: oraclelicense=accept-securebackup-cookie; " \
  http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/server-jre-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz \
  | gunzip \
  | tar -x -C /opt \
  && ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} ${JAVA_HOME}

RUN mkdir -p ${NEXUS_PERF_HOME} \
  && curl --fail --silent --location --retry 3 --output ${NEXUS_PERF_HOME}/nexus-perf.jar \
    http://repo1.maven.org/maven2/io/takari/nexus/nexus-perf/${NEXUS_PERF_CLIENT_VERSION}/nexus-perf-${NEXUS_PERF_CLIENT_VERSION}-jar-with-dependencies.jar

RUN curl --fail --silent --location --retry 3 \
    https://repo1.maven.org/maven2/io/takari/nexus/perf/data/maven/${NEXUS_PERF_DATA_SCENARIO}/${NEXUS_PERF_DATA_VERSION}/${NEXUS_PERF_DATA_SCENARIO}-${NEXUS_PERF_DATA_VERSION}-data.tar.gz \
  | gunzip \
  | tar -x -C ${NEXUS_PERF_HOME}

RUN useradd -r -u 200 -m -c "nexus role account" -d ${NEXUS_PERF_HOME} -s /bin/false nexus

VOLUME ${NEXUS_PERF_HOME}

WORKDIR ${NEXUS_PERF_HOME}
USER nexus
ENV MAX_HEAP 768m
ENV MIN_HEAP 256m
ENV JAVA_OPTS -server -Djava.net.preferIPv4Stack=true
CMD ${JAVA_HOME}/bin/java ${JAVA_OPTS} -jar nexus-perf.jar ${NEXUS_PERF_DATA_SCENARIO}-${NEXUS_PERF_DATA_VERSION}/
