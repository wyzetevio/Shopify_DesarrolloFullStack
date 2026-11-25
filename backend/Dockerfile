# Etapa 1: compilar el JAR con el wrapper de Maven
FROM eclipse-temurin:22-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: imagen ligera solo con el JRE
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Limita la memoria para el plan gratuito de Render (512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
