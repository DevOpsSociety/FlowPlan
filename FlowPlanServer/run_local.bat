@echo off
cd /d %~dp0
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/flowplan?allowPublicKeyRetrieval=true^&useSSL=false^&serverTimezone=Asia/Seoul^&characterEncoding=UTF-8
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=password
set AI_SERVICE_BASE_URL=http://localhost:8000
java -jar build\libs\FlowPlan-0.0.1-SNAPSHOT.jar
pause

