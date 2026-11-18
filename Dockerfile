# 1) build: compila o domain e o spring com Maven
# 2) execução: só o Java e o .jar, sem Maven nem código-fonte
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /build

COPY pom.xml .
COPY domain/pom.xml domain/pom.xml
COPY spring/pom.xml spring/pom.xml

COPY domain/src domain/src
COPY spring/src spring/src

RUN mvn clean package -DskipTests -pl spring -am

FROM amazoncorretto:17-alpine
LABEL MANTAINER="FATEC"
WORKDIR /app
EXPOSE 8084
COPY --from=build /build/spring/target/*.jar app.jar
# O plano gratuito do Render tem 512 MB: limita o heap para o container não ser derrubado
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-jar", "app.jar"]
