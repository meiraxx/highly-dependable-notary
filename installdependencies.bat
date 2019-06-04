REM the -f flag specifies where the pom.xml is found for the project, but I'm not sure it works for the ccUtility because of the local repo shannanigan
REM we'll just cd into dirs for now, no problem
call mvn clean compile install -f common\pom.xml
call mvn clean compile install -f communications\pom.xml