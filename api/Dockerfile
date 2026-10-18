# 1단계: jar를 레이어별로 풀기
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
COPY build/libs/hanjari-api.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
