# ChatTalkie Backend

Ktor backend for ChatTalkie with PostgreSQL and MinIO.

## Quick Start

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f ktor-api

# Stop services
docker-compose down
```

### Services

- **API**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **MinIO**: http://localhost:9000 (Console: http://localhost:9001)

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Example Requests

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","username":"testuser","password":"password123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

## Development

### Build

```bash
cd backend
./gradlew build
```

### Run Locally

```bash
./gradlew run
```

## Environment Variables

See `.env` file for configuration options.
