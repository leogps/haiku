@echo off
REM Setting up directories
set "base_dir=%CD%"
set "parent_dir=%base_dir%\.."
set "exec_dir=youtube-dl-exec"
set "exec_dir_full_path=%parent_dir%\%exec_dir%"

set "downloadable_file=yt-dlp.exe"
set "url=https://github.com/yt-dlp/yt-dlp/releases/latest/download/%downloadable_file%"
echo Downloading from %url%...

REM Creating directory if it doesn't exist
if not exist "%exec_dir_full_path%" mkdir "%exec_dir_full_path%"

set "exec_file_path=%exec_dir_full_path%\youtube-dl.exe"
powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%url%', '%exec_file_path%')"

cd /d "%exec_dir_full_path%"
youtube-dl.exe --help
