## Highly Dependable Notary Application - HDS Notary   
82014 - João Meira  
82069 - José Brás  
  
The main goal of the HDS Notary is to certify the transfer of ownership of arbitrary goods between users.  
  
---  

## Platform  
- OS: Windows 10
- Java/Javac version: javac 1.8.0_152-release  

---  

## Instructions  

### Install dependencies  
`cd common`  
`mvn clean compile install`  
`cd communications`  
`mvn clean compile install`  
`cd ccUtility`  
`mvn clean compile install`  
  
### Run notary  
`cd notary`  
`mvn clean compile install`  
  
### Run client  
`cd client`  
`mvn clean compile install`  
  
**Note:** you can also use the batch file utilities to do it automatically  
  
---  
  
### Assumptions  
1. Each user generates their keystore and extracts their public key with the use of keytool as exemplified in the batch file "generateUserKeystores.bat";  
2. Notary public key certificate is already pre-installed on every client application;  
3. Users public key certificates are shared in a meeting between users that want to communicate with each other.  
