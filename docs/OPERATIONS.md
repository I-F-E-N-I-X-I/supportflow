# Эксплуатация и диагностика

## Конфигурация

| Переменная | Назначение |
|---|---|
| DB_PASSWORD | Пароль PostgreSQL для обеих учебных баз |
| RABBIT_PASSWORD | Пароль RabbitMQ |
| REDIS_PASSWORD | Пароль Redis |
| JWT_SECRET | Ключ подписи JWT, не менее 32 байт |
| INTERNAL_TOKEN | Доступ API к worker, не менее 32 символов |
| ADMIN_PASSWORD | Пароль первоначального admin, минимум 12 символов |

Генераторы создают случайные значения, не печатая их. Bootstrap создаёт администратора только при отсутствии пользователя admin. Изменение ADMIN_PASSWORD в `.env` **не меняет пароль существующего аккаунта**. Для реального сервиса добавьте отдельную процедуру смены пароля. Также смена паролей БД/брокера в `.env` не изменяет пользователей внутри уже существующих persistent volumes — требуется согласованная ротация.

## Проверка состояния

```bash
docker compose ps
docker compose logs --tail=100 api worker
curl http://localhost:8080/actuator/health/readiness
```

API readiness проверяет жизненный цикл приложения и БД. Общий `/actuator/health` также отражает Redis/RabbitMQ, поэтому при их сбое может быть DOWN, хотя API продолжает операции с обращениями. Это намеренное разделение готовности и здоровья зависимостей.

Actuator собирает стандартные HTTP/JVM/HikariCP-метрики. Внешний доступ к `/actuator/metrics` у API запрещён политикой безопасности. Не открывайте management endpoints публично ради мониторинга; при промышленном развёртывании выделите защищённый management port или exporter. Worker разрешает метрики только с внутренним токеном.

## Журналы и персональные данные

Код не журналирует DTO, текст комментариев, username, пароли или JWT. INFO содержит технические eventId; WARN — отказ кэша или повтор доставки; ERROR — тип неожиданной ошибки и reference. SQL bind logging и Hibernate SqlExceptionHelper отключены, чтобы исключения ограничений не раскрывали введённые значения. Не включайте TRACE/DEBUG Spring Security или JDBC на окружении с реальными данными без проверки политики логирования.

## Отложенные события и DLQ

Неопубликованные события остаются в outbox. Проверить их можно локально:

```bash
docker compose exec postgres psql -U supportflow -d supportflow -c "SELECT id,attempts,next_attempt_at FROM outbox_event WHERE published_at IS NULL ORDER BY next_attempt_at;"
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

Dead-letter queue: `supportflow.notifications.dlq`. Она содержит сообщения, не обработанные после трёх попыток. Автоматическая бесконечная переотправка из DLQ не реализована: оператор должен установить причину, исправить consumer или данные и осознанно переотправить сообщение. Повтор корректного eventId не создаст второй inbox-item.

## Docker Desktop на Windows

Если Desktop завершается с `initializing Inference manager` / `dockerInference`, контейнеры проекта ещё не запущены. Это проблема окружения Docker, а не Flyway или Java. Не используйте factory reset как первый шаг: он может удалить локальные данные. Известные отчёты: [docker/desktop-feedback #448](https://github.com/docker/desktop-feedback/issues/448), [#531](https://github.com/docker/desktop-feedback/issues/531).

В среде разработки этого проекта обнаружен именно такой сбой; удаление зависшего runtime-сокета было запрещено Windows. Поэтому контейнерная проверка выполняется также в GitHub Actions на Linux. Для локального запуска требуется восстановить Docker Desktop либо использовать отдельный Linux-хост с Docker. Репозиторий не меняет настройки Docker и не удаляет ваши данные.

## Gradle и кириллица в пути Windows

В некоторых сочетаниях Windows/JDK Gradle worker не запускается из пути с кириллицей (`ClassNotFoundException: GradleWorkerMain`). Используйте JDK 21 и ASCII-путь к checkout/GRADLE_USER_HOME либо временное отображение рабочей папки свободной буквой диска через `subst`. Это проблема запуска дочерней JVM, не исключение приложения. В Linux CI используется чистый путь.

## Запуск JAR отдельно

Соберите `:api:bootJar :worker:bootJar`, затем задайте переменные окружения из application.yml и запустите:

```bash
java -jar worker/build/libs/worker-1.0.0.jar
java -jar api/build/libs/api-1.0.0.jar
```

DB_URL у API и worker должны указывать на разные базы, обе созданные заранее. Нужны доступные PostgreSQL, Redis и RabbitMQ. Compose не публикует порты зависимостей на хост: запуск JAR требует отдельной инфраструктуры или явно настроенного локального override.

## Git и CI

Работа ведётся в feature/supportflow. Pull request запускает сборку, unit-тесты, Testcontainers и Compose smoke. После исправления ошибок ветка сливается в main обычным merge, сохраняя историю осмысленных коммитов. Пример работы с конфликтом: `git fetch origin`, `git merge origin/main`, проверить конфликтующие файлы, убрать маркеры, запустить тесты, `git add <file>`, `git commit`. Искусственные конфликты и фиктивные коммиты в историю не добавляются.
