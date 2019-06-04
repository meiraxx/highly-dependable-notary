NEW STUFF:  
Important intricacy to remember:  
<url>file://${project.basedir}/lib/repo</url> IN POM  
<url>file://${project.basedir}\lib\repo</url> IN COMMANDLINE IF WINDOWS  
  
// note: this is going to be improved soon  
Template:  
`mvn deploy:deploy-file -Durl=file://path/to/repo/ -Dfile=devlib-0.1.jar -DgroupId=com.sookocheff -DartifactId=devlib -Dpackaging=jar -Dversion=0.1`   
This project:  
`mvn deploy:deploy-file -Durl=file://D:\Sandbox\SEC\highly-dependable-notary\ccUtility\lib\repo -Dfile=D:\Sandbox\SEC\highly-dependable-notary\ccUtility\lib\pteidlibj.jar -DgroupId=pt.eid -DartifactId=libj -Dpackaging=jar -Dversion=1.0`  
  
Then, clean, compile:    
`mvn clean compile`  
  
Install and execute:  
`mvn install`  
