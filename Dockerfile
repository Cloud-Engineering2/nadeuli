# Dockerfile
# 최초 작성 일자 : 2025.03.14
# 작성자 : 이홍비

FROM openjdk:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]