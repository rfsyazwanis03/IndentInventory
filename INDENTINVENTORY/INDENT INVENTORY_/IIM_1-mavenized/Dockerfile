
FROM maven :3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . . 
RUN mvn clean package -DskipTests

FROM tomcat:9.0-jdk17
RUN rm -rf /usr/local/tomat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080 
CMD ["catalina.sh" , "run"]
