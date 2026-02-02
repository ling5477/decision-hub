\
@ECHO OFF
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=.
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper jar not found. It will be downloaded.
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$p = (Get-Content '%WRAPPER_PROPERTIES%' | Where-Object { $_ -like 'wrapperUrl=*' }) -replace 'wrapperUrl=','';" ^
    "New-Item -ItemType Directory -Force -Path (Split-Path '%WRAPPER_JAR%') | Out-Null; " ^
    "Invoke-WebRequest -UseBasicParsing -Uri $p -OutFile '%WRAPPER_JAR%';"
  if errorlevel 1 (
    echo Failed to download Maven Wrapper jar. Please ensure internet access or run 'mvn -v' directly.
    exit /b 1
  )
)

java -jar "%WRAPPER_JAR%" %*
endlocal
