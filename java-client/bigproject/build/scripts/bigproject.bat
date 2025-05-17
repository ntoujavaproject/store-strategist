@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  bigproject startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and BIGPROJECT_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="--add-modules" "java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing" "--add-modules" "ALL-MODULE-PATH" "--add-opens" "java.base/java.lang=ALL-UNNAMED" "--add-opens" "javafx.graphics/javafx.scene=ALL-UNNAMED" "--add-exports" "javafx.swing/javafx.embed.swing=ALL-UNNAMED" "--illegal-access=permit" "-Xmx1g" "-Dapple.awt.application.name=Restaurant Analyzer"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\bigproject.jar;%APP_HOME%\lib\javafx-fxml-17.0.15-mac-aarch64.jar;%APP_HOME%\lib\javafx-controls-17.0.15-mac-aarch64.jar;%APP_HOME%\lib\javafx-controls-17.0.15.jar;%APP_HOME%\lib\javafx-swing-17.0.15-mac-aarch64.jar;%APP_HOME%\lib\javafx-graphics-17.0.15-mac-aarch64.jar;%APP_HOME%\lib\javafx-graphics-17.0.15.jar;%APP_HOME%\lib\javafx-base-17.0.15-mac-aarch64.jar;%APP_HOME%\lib\javafx-base-17.0.15.jar;%APP_HOME%\lib\emoji-java-5.1.1.jar;%APP_HOME%\lib\json-20240303.jar


@rem Execute bigproject
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %BIGPROJECT_OPTS%  -classpath "%CLASSPATH%" bigproject.compare %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable BIGPROJECT_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%BIGPROJECT_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
