@echo off

set out_dir=.
set out_file_ext=bas_
set tap_dir=.

FOR %%i IN (%tap_dir%\*.tap) DO (
    echo * %%i -^> %out_dir%\%%~ni.%out_file_ext%
    @java -Djava.util.logging.config.file="%~dp0logging.properties" -jar "%~dp0zxs_tap2bas.jar" -i %%i -o "%out_dir%\%%~ni.%out_file_ext%"
)
