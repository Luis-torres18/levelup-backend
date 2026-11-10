# Etapa 1: compila el proyecto con Maven (no hace falta tener Maven instalado en la EC2)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: imagen liviana que solo ejecuta el JAR
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ms-gestion-usuarios.jar app.jar
EXPOSE 8083
# Limita la memoria de la JVM (importante en instancias pequeñas como t2.micro / t3.small)
ENV JAVA_OPTS="-Xms64m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
