# 使用 Maven 並搭配 Java 21 版本來建構專案
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# 使用輕量的 Java 21 執行環境
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/myweb-0.0.1-SNAPSHOT.jar app.jar

# 告訴 Render 要監聽 8080
EXPOSE 8080

# 執行 Spring Boot 應用
ENTRYPOINT ["java", "-jar", "app.jar"]
