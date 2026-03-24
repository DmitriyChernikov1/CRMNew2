FROM maven:3.9-eclipse-temurin-21
WORKDIR /test
COPY . .
CMD ["mvn", "clean", "test"]