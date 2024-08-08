### ModelMatcher

To make changes and run :
- Make change your changes 
- Build your applciation, using `mvn clean install`
  - If you want to skip running changes add ``-DskipTests`` 
        , eg : ``mvn clean install -DskipTests``
- To start the application
    `java -Xmx1024m -Dserver.port=<PORT_TO_USE> -Dspring.profiles.active=<PROFILE_NAME> -jar target/ModelMatcher.jar`
  - `PORT_TO_USE` : You need to specify which port the application to server on, Default is `8080`,
  - `PROFILE_NAME` : Profile name specifies which app properties to use while deploying the applcaiton.
  - Available profiles : `beta`, `prod`
  - `Xmx1024m` : Specifies how much memory the application will be using, in this case `1024m` is `1GB`.


Complete command.

```
mvn clean install -DskipTests && java -Xmx1024m -Dserver.port=<PORT_TO_USE> -Dspring.profiles.active=<PROFILE_NAME> -jar target/<JAR_FILE_TO_USE>
```