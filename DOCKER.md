# Docker layout

This is a backend-first monorepo, so the backend container files live together:

```text
polymersmain/
├── docker-compose.yml       # Starts the full local stack
├── backend/
│   ├── Dockerfile           # Builds the Spring Boot API image
│   ├── .dockerignore        # Excludes local build/secrets from image context
│   └── .env.example         # Atlas override template (never commit .env)
└── frontend/                # Reserved; no frontend code yet
```

Use the root Compose file to start everything:

```powershell
docker compose up -d --build
```

It builds [`backend/Dockerfile`](backend/Dockerfile) and starts the API on port `4000`. MongoDB Atlas is the only database. To build only the API image:

```powershell
docker build -t stockai-backend ./backend
```

For Atlas, create `backend/.env` from `backend/.env.example`; Compose passes it to the backend container automatically.
