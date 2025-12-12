# docker compose 만들때 알아야할 내용들 정리

## docker compose file을 만드는 목적
- 여러개의 컨테이너들을 하나의 논리적인 서비스 단위로 묶어 한번의 명령으로 생성, 실행, 중지, 관리하기 위한 도구
- 서비스 전체를 “스택 단위”로 관리할 수 있게 해주는 자동화/오케스트레이션 도구

## .dockerignore file만들기
- .dockerignore file을 만들어 불필요한 용량을 줄여 빌드 속도를 개선함
- 보안에 민감한 정보들을 제외시킴

## port
```yaml
ports:
  - "8080:9080"
```
- \- "호스트 포트":"컨테이너 내부 포트" 로 작성 

## context
```yaml
ports:
  - "8080:9080"
```
**“도커 빌드의 작업 폴더(빌드 컨텍스트)를 현재 디렉터리로 지정한다”**는 뜻

```dockerignore
# --- VCS ---
.git
.gitignore

# --- IDE (IntelliJ) ---
.idea
*.iml
*.ipr
*.iws
out/

# --- Gradle ---
.gradle/
**/.gradle/
build/
**/build/

# --- Logs / temp ---
*.log
logs/
tmp/
temp/

# --- OS ---
.DS_Store
Thumbs.db

# --- Test / coverage ---
**/test-results/
**/reports/
**/jacoco/

# --- Env / secrets ---
.env
.env.*
*.pem
*.key
*keystore*
*truststore*

# --- Docker ---
Dockerfile*
docker-compose*.yml
```
