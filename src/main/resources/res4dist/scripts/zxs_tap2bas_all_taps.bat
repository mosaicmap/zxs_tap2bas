@echo off

FOR %%i IN (*.tap) DO (
    echo * %%i -^> %%~ni.bas
    @java -Djava.util.logging.config.file="%~dp0logging.properties" -jar "%~dp0zxs_tap2bas.jar" -i %%i -o %%~ni.bas
)


