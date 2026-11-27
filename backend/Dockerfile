# Wishka backend image. Expects the jar built by `./mvnw package` (CI builds and tests it first).
# Uses Spring Boot's layered jar so dependency layers are cached between releases.
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /build
COPY target/wishka-backend-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S wishka && adduser -S wishka -G wishka \
    && mkdir -p /data/images && chown wishka:wishka /data/images
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
USER wishka
ENV SPRING_PROFILES_ACTIVE=prod \
    IMAGE_STORAGE_PATH=/data/images \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
VOLUME /data/images
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
