FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN sed -i '/org.gradle.java.home/d' gradle.properties || true
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar -x test --no-daemon
RUN cp $(ls build/libs/*.jar | grep -v plain) /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
ENTRYPOINT ["java","-Xmx350m","-jar","app.jar"]