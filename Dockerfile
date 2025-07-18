FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ARG SENDGRID_API_KEY
ARG APP_JWT_SECRET

ENV SENDGRID_API_KEY=${SENDGRID_API_KEY}
ENV APP_JWT_SECRET=${APP_JWT_SECRET}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]