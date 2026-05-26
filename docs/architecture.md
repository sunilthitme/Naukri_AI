# Naukri AI Job Apply Bot Architecture

```mermaid
flowchart LR
  UI["Angular Material UI"] --> API["Spring Boot REST API"]
  API --> AUTH["auth-service"]
  API --> SETTINGS["settings-service"]
  API --> DASH["dashboard-service"]
  API --> BOT["naukri-automation-service"]
  BOT --> QUEUE["Async Executor"]
  BOT --> PLAY["Playwright Java POM"]
  BOT --> AI["ai-question-service"]
  AI --> LLM["OpenAI-compatible LLM"]
  BOT --> CSV["csv-service"]
  BOT --> SCHED["scheduler-service"]
  CSV --> FS["storage/csv"]
  PLAY --> SHOTS["storage/screenshots"]
  API --> DB[("PostgreSQL")]
  SCHED --> BOT
```

The backend is layered by controller, service, repository, DTO, and domain packages. Playwright automation is isolated in the `automation` Maven module so browser behavior can be tested and evolved independently from API concerns.
