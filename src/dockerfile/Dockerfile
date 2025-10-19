# Используем базовый образ с Java Runtime
FROM eclipse-temurin:17-jre

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем собранный jar-файл проекта в контейнер
COPY ./target/timeManager-1.0-SNAPSHOT.jar /app/app.jar

ENV API_OPEN_ROUTE=...
ENV TG_TOKEN=...

# Открываем порт 8080 для доступа к приложению
EXPOSE 8080

# Команда запуска приложения внутри контейнера
ENTRYPOINT ["java", "-jar", "/app/app.jar"]