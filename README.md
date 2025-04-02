# 🏞️ 나들이 - 여행 일정 관리 서비스
> 여행 계획 단계부터 결산까지 쉽고 간편하게 사용할 수 있는 서비스입니다.

<br>

## 📌 프로젝트 개요
> 회원이 손쉽게 여행 일정을 계획하고 관리하며, 경비 정산, 기행문 작성 및 최종 결산을 통합적으로 제공하는 웹 애플리케이션입니다.

<br>

## 💻 기술 스택
### Backend
- **언어** : Java 17  
- **프레임워크** : Spring Boot 3.3.5
- **DB 및 데이터 관리** : MySQL, Redis, Spring Data JPA, Spring Data JDBC
- **보안** : Spring Security 6, JWT, OAuth 2.0 (Google, Kakao 로그인 지원)
- **API** : REST API (API 문서화)
- **클라우드 및 스토리지** : AWS S3, CloudFront, AWS SDK
- **기타 라이브러리** : Lombok, Hibernate Validator, JSON

### Frontend
- **언어** : JavaScript (ES6), HTML5, CSS3
- **프레임워크 및 라이브러리** : Bootstrap

### Infra
- **서버 및 배포** : AWS EKS, Docker, AWS CloudFormation
- **컨테이너 레지스트리** : AWS ECR (Elastic Container Registry)
- **데이터베이스** : MySQL 8.0.36 (Amazon RDS)
- **캐시 서버** : Redis 7.1 (Amazon ElastiCache)
- **CI/CD** : GitHub Actions, ArgoCD
- **모니터링** : Prometheus, Grafana, Discord

<br>

## ⚙️ 주요 기능 (Features)
- **회원 관리 (OAuth 2.0 지원 로그인, 회원 정보 수정, 탈퇴)**
- **여행 일정 관리 (CRUD) 및 공유**
- **방문지 지도 표시 및 최적 경로 추천**
- **경비 기록 및 정산**
- **여행 기행문 작성**
- **여행 최종 결산**
- **이미지 업로드 및 CloudFront 캐싱 처리**

<br>

## ☁️ 배포 (Deployment)
- **CI/CD** : GitHub Actions → Docker → ECR → ArgoCD → EKS 배포
- **모니터링** : Prometheus + Grafana + Discord

<br>

## 💻 개발 환경 (Development Environment)
- **Java 17**
- **Spring Boot 3.3.5**
- **Maven 3.9.9**
- **MySQL 8.0.36**
- **Spring Security 6**
- **Spring WebFlux (일부 비동기 처리)**
- **JWT 및 OAuth 2.0 인증**
- **Redis 7.1 (세션 관리)**
- **OpenAI API 활용 (AI 기반 일정 추천 기능)**

<br>

## 👥 조원
- 조장 : **이홍비** ( redrain@yu.ac.kr )
- 조원 : **고민정** ( komj36@gmail.com )
- 조원 : **국경민** ( kookrudals@gmail.com )
- 조원 : **김대환** ( tjftjrgns@naver.com )
- 조원 : **박한철** ( parkhancheol97@gmail.com )

<br>

## 📄 application.properties

```
spring.application.name=nadeuli
logging.level.root=DEBUG
logging.level.nadeuli=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
server.port=8080
server.forward-headers-strategy=framework

# Server error
server.error.path=/error
server.error.whitelabel.enabled=false

# Actuator
spring.security.user.name=actuator
spring.security.user.password=actuator
spring.security.user.roles=ACTUATOR_ADMIN
#management.server.port=9090
management.endpoints.web.base-path=/actuator
management.endpoint.prometheus.enabled=true
management.endpoint.health.show-details=always
management.endpoints.web.exposure.include=health,info,prometheus,metrics

# DB - AWS RDS MySQL
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://[]:3306/nadeuli?serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=rootroot

# DB - Local MySQL
#spring.datasource.url=jdbc:mysql://localhost:3306/nadeuli?serverTimezone=Asia/Seoul
#spring.datasource.username=lion3
#spring.datasource.password=lion3

# DB - AWS ElastiCache Redis
spring.data.redis.host=
spring.data.redis.port=6379
# spring.data.redis.timeout=2000ms
# spring.data.redis.lettuce.pool.max-active=10
# spring.data.redis.lettuce.pool.max-idle=10
# spring.data.redis.lettuce.pool.min-idle=2
# spring.session.redis.namespace=nadeuli-session
# spring.session.store-type=redis

# DB - Local Redis
#spring.data.redis.host=localhost
#spring.data.redis.port=6379
#spring.data.redis.port=16379
spring.data.redis.connect-timeout=3600s
#(Optional) spring.data.redis.password=

# JPA
spring.jpa.generate-ddl=false
spring.jpa.hibernate.ddl-auto=none

spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=true

# Thymeleaf
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
spring.thymeleaf.check-template-location=true

# Google Map API Key
google.api.key=

# Google OAuth2
# Client ID
spring.security.oauth2.client.registration.google.client-id=
# Client Secret key
spring.security.oauth2.client.registration.google.client-secret=
# OAuth2 Client Request Scope - Profile, Email
spring.security.oauth2.client.registration.google.scope=profile, email
# OAuth2 Redirection URI
spring.security.oauth2.client.registration.google.redirect-uri=https://nadeuli.store/login/oauth2/code/google
# OAuth2 Authorization URI
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth
# Token URI
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
# User Information URI
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
# User Name Attribute
spring.security.oauth2.client.provider.google.user-name-attribute=sub


# Kakao OAuth2
kakao.api.key=
kakao.admin-key=

spring.security.oauth2.client.registration.kakao.client-id=
spring.security.oauth2.client.registration.kakao.client-secret=
spring.security.oauth2.client.registration.kakao.client-authentication-method=client_secret_post
spring.security.oauth2.client.registration.kakao.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.kakao.redirect-uri=https://nadeuli.store/login/oauth2/code/kakao
spring.security.oauth2.client.registration.kakao.scope=profile_nickname,profile_image,account_email
spring.security.oauth2.client.registration.kakao.client-name=Kakao

spring.security.oauth2.client.provider.kakao.authorization-uri=https://kauth.kakao.com/oauth/authorize
spring.security.oauth2.client.provider.kakao.token-uri=https://kauth.kakao.com/oauth/token
spring.security.oauth2.client.provider.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me
spring.security.oauth2.client.provider.kakao.user-name-attribute=id


# JWT
jwt.secret=


# ChatGPT
spring.ai.openai.api-key=
openai.model=gpt-4o


# Multipart
spring.servlet.multipart.max-file-size=30MB
spring.servlet.multipart.max-request-size=30MB


# AWS S3
# Accesss Key
cloud.aws.credentials.access-key=
# Secret Key
cloud.aws.credentials.secret-key=
# Bucket Name
cloud.aws.s3.bucket=
# Region
cloud.aws.region.static=
cloud.aws.stack.auto-=false

# CloudFront
cloud.aws.cloudfront.distribution-id=
cloudfront.url=

```
