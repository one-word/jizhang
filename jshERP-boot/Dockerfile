FROM maven:3.6-jdk-8 as java-build
WORKDIR /app

COPY . /app

RUN mkdir -p /root/.m2 \
    && echo '<settings><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Build Java application
RUN mvn clean install

FROM openjdk:8
WORKDIR /app

COPY --from=java-build /app/target/jshERP.jar /app

ENTRYPOINT ["java", "-jar", "jshERP.jar"]