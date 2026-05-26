# API Documentation

Base path: `/api`

| Method | Path | Description |
| --- | --- | --- |
| POST | `/auth/login` | JWT login |
| GET | `/dashboard` | Dashboard metrics |
| GET | `/settings/credentials` | Read saved Naukri credential metadata |
| POST | `/settings/credentials` | Save encrypted Naukri credentials |
| GET | `/settings/filters` | Read job filters |
| POST | `/settings/filters` | Save job filters |
| GET | `/settings/resumes` | List active resume profiles |
| POST | `/settings/resumes` | Add resume profile |
| GET | `/settings/scheduler` | Read scheduler config |
| POST | `/settings/scheduler` | Save scheduler config |
| GET | `/questions` | List learned answers |
| POST | `/questions` | Save answer |
| GET | `/questions/match?question=` | Test fuzzy answer match |
| GET | `/bot/status` | Current bot state |
| POST | `/bot/start` | Start automation |
| POST | `/bot/stop` | Stop automation |
| POST | `/bot/pause` | Pause automation |
| POST | `/bot/resume` | Resume automation |
| POST | `/bot/test-login` | Test Naukri login |
| POST | `/bot/test-apply` | Run dry-run apply flow |
| GET | `/jobs/history` | Applied jobs history |
| GET | `/logs` | Latest bot logs |
| GET | `/reports/export.csv` | Export applied job report CSV |
| GET | `/reports/export.xlsx` | Export applied job report Excel |
