FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/server-all.jar app.jar

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
