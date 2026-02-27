# ---- Build Stage ----
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# gradle 캐시를 위해 빌드 설정 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성만 먼저 다운로드
RUN gradle dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---- Run Stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# JVM 옵션: t3.medium(4GB) 기준
ENTRYPOINT ["java", "-Xmx2g", "-Xms1g", "-jar", "app.jar"]
