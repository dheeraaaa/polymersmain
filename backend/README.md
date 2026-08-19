# StockAI Java backend

Spring Boot 3 / Java 21 backend using MongoDB Atlas. Docker Compose runs only the API; Atlas is the sole database.

## Configure and start

1. Copy `.env.example` to `.env`, enter the Atlas username/password and a random JWT secret of 32+ characters.
2. In Atlas Network Access, allow your current IP; grant the database user `readWrite` on `stockai`.
3. Run `docker compose up -d --build` at repository root.

The API runs at `http://localhost:4000/api/v1/health`. Build/test locally with `mvn test` and package with `mvn package`.
