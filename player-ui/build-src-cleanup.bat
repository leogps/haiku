@echo off

REM Check if arguments are provided
if "%~1"=="" (
    echo Nothing to delete
    exit /b 0
)

REM Loop through each argument
:loop
if "%~1"=="" goto :eof

REM Check if item exists
if exist "%~1" (
    REM Check if item is a file
    if exist "%~1\" (
        rmdir /s /q "%~1"
        echo Deleted directory: "%~1"
    ) else (
        del /f /q "%~1"
        echo Deleted file: "%~1"
    )
) else (
    echo Item not found: "%~1"
)

shift
goto :loop
