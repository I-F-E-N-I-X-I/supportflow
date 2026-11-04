# Карта исходных требований

В исходном списке смешаны технологии приложения, знания языка и навыки кандидата. Работающий код покрывает технологии; учебный модуль даёт запускаемые примеры.

| Требования | Где реализовано или разобрано |
|---|---|
| Java 21, типы, переменные, операторы, приведения, if/else/switch, циклы, методы | api и learning-lab/Algorithms; дополнительные примеры CoreBasics |
| Классы, конструкторы, public/private/protected, static/final/this/super | Entities, Repositories.MemoryRepository/Tickets |
| Перегрузка, переопределение, String/StringBuilder, массивы | Repositories.save, Algorithms.digits, mergeSort |
| try/catch/finally/throw/throws, своё исключение | ConcurrencyLab.sumAsync, DomainException, worker listener |
| Generics, enum, ==/equals, equals/hashCode, null/Optional | Repository<K,V>, Status/Priority/Role, TicketKey, CoreJavaTest |
| Инкапсуляция, наследование, полиморфизм, абстракция, интерфейсы, композиция | JPA entities, MemoryRepository, SlaPolicy, сервисы с DI |
| List/Set/Map/Queue, ArrayList/LinkedList/HashMap/HashSet/TreeMap/TreeSet/ArrayDeque/PriorityQueue | learning-lab: Algorithms, TicketAnalytics, Repositories |
| Iterator, Comparable/Comparator, сортировка/поиск/добавление/удаление | TicketAnalytics, Algorithms, MemoryRepository |
| Stream filter/map/flatMap/sorted/distinct/limit/collect/toList/count/reduce/anyMatch/allMatch/noneMatch/findFirst/groupingBy | TicketAnalytics.summarize и тесты |
| Лямбды и ссылки на методы | Spring beans, stream pipelines, SlaPolicy |
| Массивы, связный список, стек, очередь, хеш-таблица, множества, деревья/графы | Arrays, LinkedList, ArrayDeque, HashMap, TreeMap/TreeSet, adjacency lists |
| Линейный/бинарный поиск, сортировка слиянием, два указателя, sliding window, рекурсия, DFS/BFS | Algorithms и AlgorithmsTest |
| Big O и память, хеш-таблицы | Разбор ниже и JVM.md |
| SELECT/INSERT/UPDATE/DELETE/WHERE/ORDER BY/GROUP BY/HAVING/LIMIT/JOIN/агрегаты/подзапросы/NULL | Репозитории, ReportService, миграции и sql-exercises.sql |
| PostgreSQL, PK/FK, UNIQUE/NOT NULL, One-to-One/One-to-Many/Many-to-Many, индексы, ACID, нормализация | V1 migrations, entities, ARCHITECTURE.md |
| Spring Framework/Boot, DI/IoC, Component/Service/Repository, controller mappings, DTO/Valid/Advice/ExceptionHandler/Transactional/yml | api; JpaRepository-прокси являются repository beans без обязательной явной @Repository |
| REST GET/POST/PUT/PATCH/DELETE, статусы 200/201/204/400/401/403/404/409/500 | Controllers, Errors, API.md и тесты |
| JSON, заголовки, параметры, пагинация, фильтры, сортировка, валидация | TicketController, TicketService, JWT и Idempotency-Key |
| Swagger/OpenAPI, Postman | /swagger-ui/index.html и postman/ |
| ORM, Entity/Id/GeneratedValue/Column/OneToMany/ManyToOne, JpaRepository, derived queries, @Query, lazy/eager/N+1 | api/ticket и identity; ARCHITECTURE.md |
| Git, commit/branch/merge/PR/.gitignore/README, разрешение конфликтов | История репозитория и OPERATIONS.md; реальные конфликты не имитируются |
| Maven или Gradle, зависимости, плагины, JAR, тесты | Gradle multi-project, wrapper, bootJar; Maven — предыдущий ReserveHub |
| JUnit 5, Mockito, assertions, MockMvc, unit/integration/ошибки/границы | api/src/test, worker/src/test, learning-lab/src/test |
| SOLID/DRY/KISS, слои, разделение ответственности, рефакторинг | Интерфейсы репозиториев, services/controllers, отдельный worker; паттерны ниже |
| Strategy/Factory/Builder | Policies в learning-lab; JwtClaimsSet builder в API |
| Dockerfile, образы, контейнеры, Compose, env | Dockerfile, compose.yml, scripts/init-env.* |
| Security, регистрация, BCrypt, JWT, роли | identity, Actor, InternalSecurity |
| Thread/Runnable/ExecutorService/CompletableFuture/synchronized/volatile/ConcurrentHashMap/race condition | ConcurrencyLab и tests; параллельный тест идемпотентности API |
| Монолит/микросервисы, REST, RabbitMQ, async events, retry/idempotency/error handling | API + worker, outbox, NotificationClient, ARCHITECTURE.md |
| Кэширование, TTL, временные значения, инвалидация | Redis TagCache; учебный TtlCache |
| GitHub Actions, автоматическая сборка/тесты, PR checks | .github/workflows/ci.yml |
| SLF4J/Logback, INFO/WARN/ERROR, Actuator, защита логов, метрики | event, worker, Errors, application.yml, OPERATIONS.md |
| Linux, Flyway, Testcontainers, Gradle, JVM, BigDecimal, java.time, HTTP clients | scripts, миграции, *IT, JVM.md, compensation, timestamps, NotificationClient |
| Чтение кода, debugging, объяснение, исправление ошибок | JVM.md с практическими заданиями |

## Сложность коллекций и алгоритмов

HashMap даёт ожидаемый O(1) поиск ключа против O(n) линейного поиска ArrayList. Это не обещание постоянного времени в любых условиях: хеширование, коллизии, resize и реализация ключей имеют значение. Равные ключи обязаны иметь равный hashCode; изменяемые поля ключа нельзя менять после помещения в hash-таблицу.

TreeMap/TreeSet обеспечивают порядок и O(log n) операции. ArrayDeque — амортизированное O(1) для стека/очереди по краям. LinkedList удобен для работы итератором, но доступ по индексу O(n), поэтому не стоит выбирать его по привычке. PriorityQueue поддерживает добавление/удаление O(log n), чтение вершины O(1).

Линейный поиск O(n), бинарный поиск по отсортированному массиву O(log n). Merge sort O(n log n) времени и O(n) вспомогательной памяти. Два указателя и скользящее окно O(n). BFS/DFS O(V+E) времени и O(V) служебной памяти; рекурсивный DFS может переполнить stack на глубоком графе. Top-k использует O(n log k) времени и O(k) памяти. Вложенный перебор всех пар даёт O(n²), и его стоит сравнить с two pointers или HashMap на практике.

## Почему примеры отделены

API не использует самодельную сортировку вместо базы и не создаёт Thread на каждый HTTP-запрос ради списка навыков. Учебные примеры имеют тесты и не являются частью серверного persistence-слоя. Например, учебный TtlCache не имеет фоновой очистки и ограничения размера: это упражнение, а реальный кэш проекта — Redis.
