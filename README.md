# Rabbit Email Demo (Spring Boot + RabbitMQ + Callback + Email)

A minimal, production-ish example showing:
- **Publisher** sends a `WorkRequest` to RabbitMQ and sets a **reply-to (callback)** destination.
- **Worker** consumes, does work, sends an **email notification/confirmation**, then posts a **reply** to the callback queue.
- **ReplyListener** receives the confirmation and logs it.
- **CorrelationId** is used to match request ↔ reply.
- **Dedup (idempotency)** pattern to avoid double-processing (in-memory; Redis optional).
- **Docker Compose** with **RabbitMQ Management UI** and **MailHog** (SMTP catcher) for local development.

> JDK 21 • Spring Boot 3.3.x • RabbitMQ 3.13 • MailHog (dev) • Optional Redis (prod dedup)

---

## Contents
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Windows Data Folder](#windows-data-folder)
- [Dedup / Idempotency](#dedup--idempotency)
- [Project Structure](#project-structure)
- [Logs You Should See](#logs-you-should-see)
- [Troubleshooting](#troubleshooting)
- [Notes](#notes)

---

## Architecture

```
Publisher (Spring)  --WorkRequest-->  RabbitMQ (Exchange -> work.queue)
        |                                 |
        |<-- ConfirmCallback -------------|  (broker ACK/NACK)
        |<-- ReturnCallback --------------|  (unroutable when mandatory=true)
        |
        v
Worker (Spring)  -- does work -->  sends Email (MailHog in dev)
        |
        |-- Reply (WorkConfirmation + CorrelationId)
        v
RabbitMQ (Exchange -> reply.queue) --> ReplyListener (Spring)
```

**CorrelationId** flows: Publisher → Worker → Reply → ReplyListener to match requests with replies.

---

## Quick Start

### 1) Start infra (RabbitMQ + MailHog)
```bash
docker compose up -d
```
- RabbitMQ UI: http://localhost:15672  (user/pass: `guest`/`guest`)
- MailHog UI:  http://localhost:8025    (emails captured here)

### 2) Run the app
```bash
./mvnw spring-boot:run
```
> The app sends a sample `WorkRequest` on startup via `CommandLineRunner`.

### 3) Verify
- **Console logs** show publish → worker → email → reply.
- **MailHog UI** shows the email notification.

---

## Configuration

`src/main/resources/application.yml` (dev defaults for MailHog):
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    publisher-confirm-type: correlated
    template:
      mandatory: true

  mail:
    host: localhost   # MailHog SMTP
    port: 1025
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false

notification:
  from: "no-reply@example.com"
  to: "ops@example.com"
```

**Real SMTP (prod example)** — create `application-prod.yml`:
```yaml
spring:
  mail:
    host: smtp.your-provider.com
    port: 587
    username: ${SMTP_USER}
    password: ${SMTP_PASS}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```
Run with prod profile:
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

**Publisher confirms & returns**
- `publisher-confirm-type: correlated` → per-message ACK/NACK via `ConfirmCallback`.
- `template.mandatory: true` → unroutable messages trigger `ReturnsCallback` (not silently dropped).

---

## Windows Data Folder

Prefer a **bind mount** to store RabbitMQ data on your C: drive:
```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - "C:/docker-data/rabbitmq:/var/lib/rabbitmq"  # <— Windows path
```
> Use forward slashes `C:/...` in Compose. Create the folder first: `mkdir C:\docker-data\rabbitmq`

---

## Dedup / Idempotency

To avoid double-processing (e.g., redeliveries), the worker uses an **idempotency key**:
- Prefer `CorrelationId`; fallback to `jobId`.
- In-memory dedup stores seen keys for a TTL (default 1 hour).
- For production, switch to **Redis** and use atomic `SETNX` with TTL.

In-memory config (already included):
```yaml
dedup:
  ttl-seconds: 3600
```

Optional Redis service (add to `docker-compose.yml`):
```yaml
redis:
  image: redis:7
  ports:
    - "6379:6379"
```

---

## Project Structure

```
rabbit-email-demo/
├─ docker-compose.yml
├─ pom.xml
├─ src/main/java/com/example/rabbit/
│  ├─ RabbitEmailDemoApplication.java
│  ├─ RabbitConfig.java
│  ├─ model/
│  │  ├─ WorkRequest.java           # jobId, payload, notifyEmail
│  │  └─ WorkConfirmation.java      # jobId, status, details
│  └─ service/
│     ├─ PublisherService.java      # sets reply-to, CorrelationId, sends message
│     ├─ WorkerListener.java        # processes, sends email, replies with same CorrelationId
│     ├─ ReplyListener.java         # logs callback/confirmation
│     └─ EmailService.java          # JavaMailSender wrapper
└─ src/main/resources/
   └─ application.yml               # Rabbit + MailHog SMTP
```

---

## Logs You Should See

```
[PUBLISHER] Sent job JOB-EMAIL-1 corrId=...
[BROKER-CONFIRM] Ack for correlationId=...
[WORKER] Processing jobId=JOB-EMAIL-1 payload=Hello with email!
[EMAIL] Sent confirmation to test@example.com for jobId=JOB-EMAIL-1
[WORKER] Replied for corrId=...
[CALLBACK] corrId=... jobId=JOB-EMAIL-1 status=SUCCESS details=Processed payload length=...
```

---

## Troubleshooting

- **`incompatible types: String cannot be converted to Address`**  
  Use `props.setReplyToAddress(new org.springframework.amqp.core.Address(EXCHANGE, REPLY_RK));`

- **Emails not visible**  
  Ensure MailHog is running (`docker compose up -d`) and app uses `host: localhost`, `port: 1025`.

- **RabbitMQ UI not loading**  
  Port `15672` must be free. Check container logs: `docker logs rabbitmq`.

- **Unroutable message**  
  With `mandatory: true`, you’ll see `[RETURNED] ...`; check exchange, binding, and routing key.

- **Windows bind mount permission**  
  Create the folder first and run Docker Desktop with proper file sharing permissions.

---

## Notes

- The app currently publishes a sample message on startup via `CommandLineRunner`. Replace with your own trigger or expose a REST endpoint as needed.
- For **production**: consider DLX for poison messages, manual acks, retry/backoff, and Redis-based dedup.
- CorrelationId acts like a **tracking number** for request/response matching across services.

---

Happy hacking!
