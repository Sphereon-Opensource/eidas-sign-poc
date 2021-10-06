FROM adoptopenjdk/openjdk13:jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 21762
ENTRYPOINT ["java","-jar","/app.jar"]
