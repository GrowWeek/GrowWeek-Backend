# 1. 베이스 이미지 설정 (최신 Ubuntu)
FROM openjdk:24-ea-oraclelinux9

# 3. 환경 변수 설정
ENV JAVA_HOME=/opt/jdk-24
ENV PATH="$JAVA_HOME/bin:$PATH"

# 4. 작업 디렉토리 생성
WORKDIR /app

# 5. CI에서 빌드된 JAR 파일을 이미지로 복사
# CI 설정에 따라 JAR 파일 경로(build/libs/*.jar 등)를 맞게 수정하세요.
COPY build/libs/*.jar app.jar

# 6. 포트 노출 및 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]