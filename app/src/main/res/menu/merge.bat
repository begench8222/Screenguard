@echo off
break > result.txt
for %%f in (*.xml) do (
    echo [FILE: %%f] >> result.txt
    type "%%f" >> result.txt
    echo. >> result.txt
)