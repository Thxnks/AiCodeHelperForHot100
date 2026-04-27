# API Contract Examples

Base URL: `/api`

## Register

POST `/auth/register`

Request:
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Passw0rd!"
}
```

Response:
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "alice",
      "email": "alice@example.com",
      "status": "ACTIVE",
      "role": "USER",
      "createdAt": "2026-04-26T10:30:00"
    }
  },
  "timestamp": 1714098600000
}
```

## Refresh Token

POST `/auth/refresh`

Request:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## Error Code Dictionary

GET `/meta/error-codes`

Response item:
```json
{
  "code": 40001,
  "httpStatus": 400,
  "category": "COMMON",
  "message": "Validation failed"
}
```

## Validation Error Sample

POST `/auth/login`

Request:
```json
{
  "username": "",
  "password": ""
}
```

Response:
```json
{
  "code": 40001,
  "message": "username: username cannot be blank; password: password cannot be blank",
  "data": null,
  "timestamp": 1714098600000
}
```
