# ========================================
# Step 1: 빌드 단계 (JAR 파일 만들기)
# ========================================
FROM amazoncorretto:17-alpine AS builder

# 작업 디렉토리 생성
WORKDIR /app

# Gradle 관련 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 소스 코드 복사
COPY src src

# JAR 파일 빌드 (테스트는 건너뛰기)
# bootJar: 실행 가능한 JAR만 생성 (plain.jar 생성 안함)
RUN ./gradlew clean bootJar -x test --no-daemon

# 빌드된 JAR 파일 이름을 app.jar로 변경
RUN mv build/libs/*.jar app.jar

# ========================================
# Step 2: 실행 단계 (JAR 파일 실행하기)
# ========================================
FROM amazoncorretto:17-alpine

# 작업 디렉토리 생성
WORKDIR /app

# 한국 시간대 설정
ENV TZ=Asia/Seoul

# 이전 단계에서 만든 JAR 파일 가져오기
COPY --from=builder /app/app.jar app.jar

# 애플리케이션이 사용할 포트 열기
EXPOSE 9080

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

# ========================================
# 사용 방법
# ========================================
#
# 1. 이미지 빌드
#    docker build -t myauth .
#
# 1-1. 버전별 빌드(optional)
#    docker build -t myauth:v1.0 .
#    docker build -t myauth:v2.0 .
#    docker build -t myauth:v3.0 .
#
# 2. 컨테이너 실행 (백그라운드, 로컬 MySQL 연결)
#    docker run -d --name myauth \
#      -p 9080:9080 \
#      -e DB_URL=jdbc:mysql://host.docker.internal:3306/mannal \
#      -e DB_USERNAME=root \
#      -e DB_PASSWORD=1234 \
#      myauth
#
#    옵션 설명:
#    -d : 백그라운드 실행 (detached mode)
#    --name myauth : 컨테이너 이름 지정
#    host.docker.internal : Mac/Windows에서 호스트의 localhost를 가리킴
#                           (Linux는 --add-host=host.docker.internal:host-gateway 옵션 추가)
#
# 3. 로그 확인
#    docker logs myauth              # 전체 로그 확인
#    docker logs -f myauth           # 실시간 로그 팔로우 (Ctrl+C로 종료)
#    docker logs --tail 100 myauth   # 마지막 100줄만 확인
#    docker logs --since 10m myauth  # 최근 10분 로그만 확인
#
# 4. 중지 및 삭제
#    docker stop myauth
#    docker rm myauth
#
# ========================================
