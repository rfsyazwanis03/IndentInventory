# Stage 1: Build WAR using Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source files (only after dependencies are cached)
COPY . .

# Build WAR (skip tests)
RUN mvn clean package -DskipTests

# Stage 2: Deploy to Tomcat
FROM tomcat:9.0-jdk17

# Clean default apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy built WAR to ROOT
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
