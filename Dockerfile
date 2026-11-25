# Imagen del backend de ShopCloud para desplegar (Render u otro servicio con Docker).
# Se construye desde la raíz del repositorio porque también copia los scripts de database/.

FROM eclipse-temurin:22-jdk AS build
WORKDIR /app
COPY backend/ .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:22-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY database/*.sql database/
COPY backend/docker-entrypoint.sh .
RUN sed -i 's/\r$//' docker-entrypoint.sh && chmod +x docker-entrypoint.sh
# Memoria para el plan gratuito de Render (512 MB en total): heap fijo y tope al resto
# (metaspace, código JIT, pilas de hilos). Con un porcentaje del total no queda sitio para lo demás.
ENV JAVA_OPTS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
