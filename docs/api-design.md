# API Design

Base URL: `/api/v1`
All request and response bodies are JSON.
All endpoints except `/auth/**` require a JWT in the `Authorization: Bearer <token>` header.

## Conventions

- Resource paths are plural nouns: `/expenses`, `/categories`.
- `201 Created` on successful creation, `200 OK` on read/update, `204 No Content` on delete.
- All list endpoints return items owned by the authenticated user only. Requesting
  another user's resource by ID returns `404` (not `403`, to avoid confirming existence).
- Dates are ISO 8601: `2026-07-07`. Money is a decimal number with 2 fraction digits.

## Standard error shape

Every non-2xx response uses this body:

```json
{
  "timestamp": "2026-07-07T10:15:30Z",
  "status": 400,
  "message": "Validation failed",
  "errors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

`errors` is present only for validation failures (400); otherwise it is omitted.

Common statuses:

| Status | Meaning |
|--------|---------|
| 400 | Validation failed (bad input shape or values) |
| 401 | Missing/invalid/expired token |
| 404 | Resource does not exist or is not owned by the caller |
| 409 | Conflict with a uniqueness or integrity rule |

---

## Auth

### POST /auth/register

Creates a user and seeds their default categories in the same transaction.

Request:
```json
{
  "email": "budi@example.com",
  "username": "budi",
  "password": "correct-horse-battery",
  "displayName": "Budi"
}
```

Validation: email format; username 3-30 chars, `[a-z0-9_]`, stored lowercase;
password min 8 chars; displayName 1-100 chars.

Responses:
- `201` → `{ "id": 1, "email": "...", "username": "...", "displayName": "..." }`
- `409` → email or username already taken. Message states which:
  `"Email already registered"` / `"Username already taken"`.

### POST /auth/login

Request:
```json
{
  "identifier": "budi",
  "password": "correct-horse-battery"
}
```

`identifier` accepts email or username. Resolution rule: if it contains `@`,
look up by email; otherwise by username.

Responses:
- `200`:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { "id": 1, "username": "budi", "displayName": "Budi" }
}
```
- `401` → `"Invalid credentials"` (same message whether the identifier or the
  password was wrong, to avoid confirming account existence).

### POST /auth/refresh

Request: `{ "refreshToken": "eyJ..." }`
Responses: `200` with a new token pair, or `401` if the refresh token is
invalid or expired (client must log in again).

Token lifetimes: access 15 minutes, refresh 30 days.

---

## Categories

### GET /categories

Returns all categories owned by the caller, ordered by name.

`200`:
```json
[
  { "id": 1, "name": "Food", "color": "#E8593C" },
  { "id": 2, "name": "Transport", "color": "#3B8BD4" }
]
```

### POST /categories

Request: `{ "name": "Coffee", "color": "#7F5539" }`
Validation: name 1-50 chars, trimmed; color optional, `#RRGGBB` format.

Responses:
- `201` with the created category
- `409` → `"You already have a category with this name"`

### PUT /categories/{id}

Request: same shape as POST. Responses: `200`, `404`, `409` (rename collides
with an existing category name).

### DELETE /categories/{id}

Responses:
- `204` on success
- `404` if not found / not owned
- `409` if the category still has expenses:
  `"Category has N expenses. Move or delete them first."`
  (This surfaces the DB ON DELETE RESTRICT rule as a friendly API error.)

---

## Expenses

### GET /expenses?month=2026-07&categoryId=3&page=0&size=20

All query params optional:

| Param | Meaning | Default |
|-------|---------|---------|
| `month` | `YYYY-MM`, filters by expense_date within that month | current month |
| `categoryId` | filter to one category | all |
| `page`, `size` | zero-based pagination | 0, 20 (max size 100) |

`200`:
```json
{
  "items": [
    {
      "id": 10,
      "amount": 45000.00,
      "expenseDate": "2026-07-05",
      "note": "lunch",
      "category": { "id": 1, "name": "Food", "color": "#E8593C" }
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 34,
  "totalPages": 2
}
```

Sorted by `expenseDate` descending, then `id` descending (stable order for
same-day expenses).

### POST /expenses

Request:
```json
{
  "amount": 45000.00,
  "categoryId": 1,
  "expenseDate": "2026-07-05",
  "note": "lunch"
}
```

Validation: amount > 0, max 10 digits before the decimal; categoryId must
exist AND belong to the caller (else `404`); expenseDate required, not more
than 1 day in the future; note optional, max 500 chars.

Responses: `201` with the created expense (same shape as list items), `400`, `404`.

### PUT /expenses/{id}

Request: same shape as POST. Responses: `200`, `400`, `404`.

### DELETE /expenses/{id}

Responses: `204`, `404`.

---

## Summary

### GET /summary?month=2026-07

Powers the dashboard: monthly total, per-category breakdown, and the
comparison with the previous month.

`200`:
```json
{
  "month": "2026-07",
  "total": 2450000.00,
  "previousMonthTotal": 2810000.00,
  "changePercent": -12.8,
  "byCategory": [
    { "categoryId": 1, "name": "Food", "color": "#E8593C", "total": 900000.00, "percent": 36.7 },
    { "categoryId": 2, "name": "Transport", "color": "#3B8BD4", "total": 550000.00, "percent": 22.4 }
  ]
}
```

Notes:
- `changePercent` is null when the previous month total is 0 (no division by zero;
  the frontend shows "no data for last month" instead).
- `byCategory` includes only categories with expenses that month, sorted by
  total descending.
- One endpoint instead of separate total/breakdown/comparison endpoints: the
  dashboard needs all three together, so one round trip.

---

## Auth flow (JWT)

1. `POST /auth/login` → client receives access token (15 min) + refresh token (30 days).
2. Client sends `Authorization: Bearer <accessToken>` on every request.
3. On `401` due to expiry, client calls `POST /auth/refresh` once, retries the
   original request with the new access token.
4. If refresh also returns `401`, client clears tokens and redirects to login.

Storage (frontend): tokens kept in memory + refresh token in `localStorage`.
Trade-off vs httpOnly cookies documented in design-decisions.md once implemented.

## Open questions / deferred

- Rate limiting on /auth endpoints: out of scope v1, note for production hardening.
- PATCH vs PUT for partial updates: v1 uses PUT with full body for simplicity.
- Logout endpoint / refresh token revocation list: deferred; v1 relies on
  short access token lifetime.
