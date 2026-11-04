# REST API

Интерактивная спецификация генерируется из кода: `/swagger-ui/index.html`, JSON — `/v3/api-docs`. Базовый путь `/api/v1`. Для защищённых маршрутов передавайте `Authorization: Bearer <accessToken>`. Тела запросов — `application/json`. JWT не передаётся в URL.

| Метод и путь | Доступ | Назначение |
|---|---|---|
| POST /auth/register | Публичный | username/password, создание CLIENT, 201 |
| POST /auth/login | Публичный | accessToken, tokenType, expiresIn |
| GET /auth/me | Авторизованный | Текущий аккаунт |
| PATCH /admin/users/{id}/role | ADMIN | CLIENT или SUPPORT |
| POST /tickets | Авторизованный | Создать своё обращение; нужен Idempotency-Key, 201 |
| GET /tickets | CLIENT: свои; staff: все | Поиск, фильтры, сортировка, пагинация |
| GET /tickets/{id} | Владелец или staff | Детали |
| PUT /tickets/{id} | Владелец или staff | Полная замена редактируемых полей в NEW, проверка version |
| PATCH /tickets/{id}/assignee | Staff | Назначение; SUPPORT только на себя |
| PATCH /tickets/{id}/status | По правилам переходов | Изменить статус |
| PATCH /tickets/{id}/compensation | ADMIN | Установить точную сумму |
| GET /tickets/{id}/tags | Владелец или staff | Теги обращения |
| POST /tickets/{id}/comments | Владелец или staff | Комментарий; internal только staff, 201 |
| GET /tickets/{id}/comments | Владелец или staff | Пагинация, скрытие внутренних комментариев |
| GET /tickets/{id}/history | Владелец или staff | История статусов |
| GET /tags | Авторизованный | Кэшируемый справочник |
| POST /tags | ADMIN | Создать тег, 201 |
| DELETE /tags/{id} | ADMIN | Удалить неиспользуемый тег, 204 |
| GET /reports/summary | Staff | Количество, сумма компенсаций, среднее время до решения |
| GET /reports/workload?minimum=0 | Staff | Открытые обращения по сотрудникам |
| GET /notifications | Авторизованный | Только собственный inbox из worker |

## Регистрация и login

```json
{"username":"alice","password":"replace-with-your-password"}
```

Username: 3–64 латинских буквы, цифры, `_`, `-`; регистр значим. Пароль: 12–64 печатных ASCII-символа без пробелов. Ограничение исключает неявное усечение BCrypt. Роль из тела запроса не принимается. Неизвестные JSON-поля дают 400.

## Создание обращения

Заголовок: `Idempotency-Key: request-2026-0001`. Разрешено 8–80 латинских букв, цифр, `_`, `-`.

```json
{"title":"Ошибка в счёте","description":"Сумма не совпадает с заказом","priority":"HIGH","tagIds":[1]}
```

Title: непустой, до 160 символов; description: непустой, до 4000; priority: LOW/NORMAL/HIGH/URGENT; tagIds обязателен, можно `[]`, максимум 10. Повтор с тем же ключом и нормализованным телом возвращает тот же ticket и 201. Повтор с другим телом — 409.

## Изменения

PUT `/tickets/{id}` принимает title, description, priority, tagIds и `version`, полученный из GET. Несовпадение версии — 409. PATCH назначения: `{"assigneeId":3}`. PATCH статуса: `{"status":"IN_PROGRESS"}`. Комментарий: `{"body":"Проверяю обращение","internal":true}`. Компенсация: `{"amount":123.45}`, от 0 до 9999999999.99, не более двух знаков после точки; условная валюта RUB, поле не запускает платёж.

## Поиск

```text
/tickets?query=счёт&status=NEW&priority=HIGH&assigneeId=3&page=0&size=20&sort=createdAt&ascending=false
```

Page: 0–10000; size: 1–100. Sort: id, title, createdAt, updatedAt. Подстрочный поиск нечувствителен к регистру; `%` и `_` ищутся буквально. Фильтры комбинируются через AND. Ответ: `items`, `page`, `size`, `total`. Inbox вместо total возвращает items/page/size; история статусов возвращается списком. Даты — ISO 8601 UTC. Время обработки в отчёте — от создания до последнего решения, в секундах; для нерешённых значение отсутствует. Повторное открытие очищает resolvedAt.

## Ошибки

400 — неверные данные/параметры; 401 — неверные или отсутствующие учётные данные; 403 — запрещённое действие; 404 — ресурс отсутствует или чужое обращение; 409 — версия, переход, повтор ключа с другим телом, ограничение базы; 500 — непредвиденная ошибка с техническим reference; 503 — inbox временно недоступен.

Доменные ошибки используют Problem Details. Ошибки JWT обрабатывает Spring Security. В теле ошибки не возвращаются SQL, stack trace, пароли или содержимое обращения.
