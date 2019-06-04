REM generate "normal" notary key pair

for /l %%i in (1,1,7) do (
	call keytool -genkeypair -alias notaryTest%%ikey -keyalg RSA -keysize 2048 -dname "CN=notaryTest%%i, OU=notaryTest%%i, O=notaryTest%%i, L=Lisbon, ST=Portugal, C=PT" -keypass 123456 -validity 100 -storetype JKS -keystore notaryTest%%ikeystore.jks -storepass abcdef
	call keytool -exportcert -alias notaryTest%%ikey -keypass 123456 -storetype JKS -keystore notaryTest%%ikeystore.jks -file notaryTest%%i.pem -rfc -storepass abcdef
)