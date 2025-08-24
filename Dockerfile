# Dockerfile

# 1. 베이스 이미지 선택 (Java 17 JRE가 포함된 가벼운 이미지)
FROM openjdk:17-jdk-slim

# 2. JAR 파일 경로를 인자(ARG)로 지정 (빌드 도구에 따라 경로 수정)
ARG JAR_FILE_PATH=build/libs/*.jar

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. 빌드된 JAR 파일을 이미지 안으로 복사
COPY ${JAR_FILE_PATH} app.jar

# 5. JVM 메모리 옵션 설정 (메모리 부족 시 이 값을 조절하세요)
# JAVA_TOOL_OPTIONS는 JVM이 자동으로 인식하는 표준 환경 변수입니다.
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m"

# 6. 애플리케이션 실행 포트 노출 (Spring Boot 기본 포트)
EXPOSE 8443

# 7. 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]