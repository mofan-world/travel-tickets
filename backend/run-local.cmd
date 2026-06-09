@echo off
setlocal
set "PROJECT_ROOT=%~dp0..\..\.."
set "JAVA_HOME=%PROJECT_ROOT%\tools\jdk\jdk-17.0.19+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

"%JAVA_HOME%\bin\java.exe" -Xms64m -Xmx256m ^
  -jar "%~dp0target\travel-ticket-service-0.1.0-SNAPSHOT.jar" ^
  --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/travel_ticket?sslmode=disable ^
  --spring.datasource.username=travel_ticket ^
  --spring.datasource.password=travel_ticket ^
  --spring.data.redis.host=127.0.0.1 ^
  --spring.data.redis.port=6379 ^
  --spring.data.elasticsearch.repositories.enabled=false ^
  --management.health.elasticsearch.enabled=false ^
  --spring.cloud.nacos.discovery.enabled=false ^
  --spring.cloud.nacos.config.enabled=false
