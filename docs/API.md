# StockAI API reference (Java backend)

Base URL: `http://localhost:4000/api/v1`. Except for health and authentication, send `Authorization: Bearer <token>`. JSON requests require `Content-Type: application/json`.

| Method | Path | Purpose | Roles |
|---|---|---|---|
| GET | `/health` | Liveness check | Public |
| POST | `/auth/register` | Create user and receive JWT | Public - bootstrap only; restrict after initial setup |
| POST | `/auth/login` | Authenticate and receive a signed JWT | Public |
| GET, POST | `/materials` | List/create material master records | Authenticated / admin, store, purchase |
| GET, PATCH | `/materials/:id` | Read/update material | Authenticated / admin, store, purchase |
| GET, POST | `/batches` | List batches / receive a batch | Authenticated / admin, store, warehouse |
| POST | `/batches/:id/release` | Release QC-cleared batch | Admin, quality |
| POST | `/movements` | Consume, adjust, scrap, dispatch, or add production output | Operational roles |
| GET | `/dashboard/summary` | Inventory/reorder/quarantine summary | Authenticated |

## Postman quick start

Create an environment variable `baseUrl` with `http://localhost:4000/api/v1`, then call `POST {{baseUrl}}/auth/register`:

```json
{"name":"System Admin","email":"admin@example.com","password":"correct-horse-battery-staple","role":"SUPER_ADMIN"}
```

Save `accessToken` from the response as the `token` environment variable. Every protected request requires `Authorization: Bearer {{token}}`. The backend verifies signed JWTs and checks role permissions at the route level.

### Create material

`POST {{baseUrl}}/materials`

```json
{"sku":"PP-HOMO-001","name":"Polypropylene Homopolymer","category":"RAW_MATERIAL","unit":"KG","reorderPoint":500,"leadTimeDays":7}
```

### Receive a batch

`POST {{baseUrl}}/batches`

```json
{"batchNumber":"PP-20260814-001","materialId":"<material-id>","supplierLot":"SUP-LOT-91","warehouseId":"RAW-WH","unitId":"UNIT-1","quantity":2500,"uom":"KG","barcode":"PP-20260814-001"}
```

New batches start in `QUARANTINE`; release a QC-cleared batch via `POST /batches/:id/release`. Quantity-changing movement bodies use `{ "batchId":"<id>", "type":"CONSUMPTION", "quantity":25, "reference":"RUN-001" }`. The permitted types are `CONSUMPTION`, `PRODUCTION_OUTPUT`, `SCRAP`, `ADJUSTMENT`, and `DISPATCH`.

### Transfer lifecycle

Create: `POST /transfers` with `batchId`, `fromWarehouseId`, `toWarehouseId`, and `quantity`; then call `/approve`, then `/receive`. Transfer state is enforced server-side.

Errors follow `{ "error": { "code": "...", "message": "..." } }`, using 400 for invalid input, 401/403 for access, 404 for missing records, and 409 for duplicate or insufficient-stock conflicts.
