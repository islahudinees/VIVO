FROM maven:3-openjdk-11 AS builder

WORKDIR /app

COPY . /app

RUN mvn clean install

FROM tomcat:9-jdk11-openjdk

ENV JAVA_OPTS="${JAVA_OPTS} -Dvivo-dir=/opt/vivo/home/"

RUN mkdir /opt/vivo
RUN mkdir /opt/vivo/home

COPY --from=builder /app/installer/webapp/target/vivo.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
