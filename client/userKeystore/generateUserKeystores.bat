REM Keytool Guides:
REM 1. https://docs.oracle.com/cd/E19509-01/820-3503/gfzbf/index.html
REM 2. http://tutorials.jenkov.com/java-cryptography/keytool.html

REM run this to generate 5 keyStores and extract their PEM certificate
REM this program assumes your JAVA_HOME/bin folder is in the environment variables! You can find the keytool there.

for /l %%i in (1,1,5) do (
	call keytool -genkeypair -alias User%%ikey -keyalg RSA -keysize 2048 -dname "CN=User%%i, OU=User%%i, O=User%%i, L=Lisbon, ST=Portugal, C=PT" -keypass 123456 -validity 100 -storetype JKS -keystore User%%ikeystore.jks -storepass abcdef
	call keytool -exportcert -alias User%%ikey -keypass 123456 -storetype JKS -keystore User%%ikeystore.jks -file User%%i.pem -rfc -storepass abcdef
)
