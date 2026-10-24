@echo off
rem ---------------------------------------------------------------------------
rem  TP4 - Hibernate et persistance des donnees
rem  Lance le scenario de demonstration contre la base MySQL/MariaDB locale.
rem  Prerequis : JDK 17+, base "db_tp_hibernate" creee, serveur MySQL demarre.
rem ---------------------------------------------------------------------------
chcp 65001 >nul
title TP4-Hibernate-Persistance
mode con: cols=140 lines=42 >nul 2>&1
cd /d "%~dp0"

java -version 2>&1 | findstr /c:"1.8.0" >nul && goto :java_trop_ancien

if not exist "target\classes\app\Demo.class" goto :compiler
if not exist "target\lib" goto :compiler
goto :executer

:compiler
echo Compilation du projet...
call mvn -q clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [ECHEC] Compilation impossible : verifiez que JAVA_HOME pointe vers un JDK 17+.
    pause
    exit /b 1
)

:executer
rem Format de journal compact : une ligne par message Hibernate.
set "LOG_FORMAT=[%%4$s] %%5$s%%n"
java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstderr.encoding=UTF-8" "-Djava.util.logging.SimpleFormatter.format=%LOG_FORMAT%" -cp "target\classes;target\lib\*" app.Demo
set "CODE=%ERRORLEVEL%"
echo.
echo Code de sortie : %CODE%
pause
exit /b %CODE%

:java_trop_ancien
echo [ECHEC] Java 8 a ete detecte sur le PATH.
echo         Ce projet necessite un JDK 17 ou superieur (java -version pour verifier).
pause
exit /b 1