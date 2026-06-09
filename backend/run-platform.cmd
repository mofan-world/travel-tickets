@echo off
setlocal
cd /d "%~dp0"
set "PROJECT_ROOT=%~dp0..\..\.."
set "JAVA_HOME=%PROJECT_ROOT%\tools\jdk\jdk-17.0.19+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "NACOS_ENABLED=true"
set "NACOS_SERVER_ADDR=127.0.0.1:8848"
set "ES_ENABLED=true"
set "ES_URIS=http://127.0.0.1:9200"

"%JAVA_HOME%\bin\java.exe" -Xms128m -Xmx512m ^
  -jar "%~dp0target\travel-ticket-service-0.1.0-SNAPSHOT.jar" ^
  --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/travel_ticket?sslmode=disable ^
  --spring.datasource.username=travel_ticket ^
  --spring.datasource.password=travel_ticket ^
  --spring.data.redis.host=127.0.0.1 ^
  --spring.data.redis.port=6379
