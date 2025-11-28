FROM eclipse-temurin:21-jdk AS plugin-builder

RUN apt-get update && \
    apt-get install -y git bash findutils && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY opensearch-hebrew-analyser .
RUN ./gradlew --no-daemon clean assemble
