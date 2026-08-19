# StockAI Backend API Information

## Base URL

```text
http://localhost:4000/api/v1
```

All requests and responses use JSON. Protected endpoints require:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Get `accessToken` from the login response.

## Roles

`SUPER_ADMIN`, `FACTORY_DIRECTOR`, `PLANT_MANAGER`, `STORE_MANAGER`, `PURCHASE_MANAGER`, `PRODUCTION_MANAGER`, `QUALITY_MANAGER`, `WAREHOUSE_EXECUTIVE`, `DISPATCH_EXECUTIVE`

## Standard errors

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": { "field": "message" }
  }
}
```

Common status codes: `400` invalid request, `401` missing/invalid token, `403` insufficient role, `404` record missing, `409` duplicate/invalid inventory state/insufficient stock, `500` unexpected error.

## Health

### `GET /health`

Public service check.

```json
{
  "status": "ok",
  "service": "stockai-backend",
  "timestamp": "2026-08-15T00:00:00Z"
}
```

## Authentication

### `POST /auth/register`

Bootstraps the first `SUPER_ADMIN` and returns a JWT. Once a super-admin exists, this endpoint is closed; create staff through `POST /users`.

```json
{
  "name": "Store Manager",
  "email": "store@example.com",
  "password": "at-least-12-characters",
  "role": "STORE_MANAGER",
  "unitId": "UNIT-1"
}
```

Returns `201 Created`:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "user": {
    "id": "<user-id>",
    "name": "Store Manager",
    "email": "store@example.com",
    "role": "STORE_MANAGER",
    "unitId": "UNIT-1"
  }
}
```

### `POST /auth/login`

```json
{
  "email": "store@example.com",
  "password": "at-least-12-characters"
}
```

Returns the same token response as registration. Tokens expire after eight hours by default (`JWT_EXPIRY_SECONDS`).

## Materials

### `GET /materials`

Authenticated. Returns all material master records.

### `GET /materials/{id}`

Authenticated. Returns one material.

### `POST /materials`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `PURCHASE_MANAGER`.

```json
{
  "sku": "PP-HOMO-001",
  "name": "Polypropylene Homopolymer",
  "category": "RAW_MATERIAL",
  "unit": "KG",
  "reorderPoint": 500,
  "leadTimeDays": 7
}
```

Returns `201 Created`. `sku` is normalized to uppercase and must be unique.

### `PUT /materials/{id}`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `PURCHASE_MANAGER`. Uses the same complete body as material creation.

## Warehouses and staff assignment

Warehouses are master records. MongoDB assigns each warehouse a unique `id`; the backend additionally creates a unique readable code such as `WH-4A3F19C2`. Batches and transfers use the warehouse `id`, not its code.

### `GET /warehouses`

Authenticated. Lists warehouse records.

### `GET /warehouses/{id}`

Authenticated. Gets one warehouse.

### `POST /warehouses`

Roles: `SUPER_ADMIN`, `PLANT_MANAGER`, `STORE_MANAGER`.

```json
{
  "name": "Raw Material Warehouse",
  "unitId": "UNIT-1",
  "type": "RAW_MATERIAL",
  "address": "Unit 1, Bay A",
  "capacity": 50000,
  "capacityUnit": "KG"
}
```

Returns `201 Created`, including generated `id` and `code`.

### `PUT /warehouses/{id}`

Roles: `SUPER_ADMIN`, `PLANT_MANAGER`, `STORE_MANAGER`. Replaces the editable warehouse details with the same body as creation.

### `PATCH /warehouses/{id}/active?value=false`

Roles: `SUPER_ADMIN`, `PLANT_MANAGER`.

Activates or deactivates a warehouse. Deactivated warehouses cannot be used for stock operations.

### `PUT /warehouses/{id}/staff`

Roles: `SUPER_ADMIN`, `PLANT_MANAGER`, `STORE_MANAGER`.

Replaces the warehouse's staff assignment list. Supply MongoDB user IDs:

```json
{
  "userIds": ["<store-user-id>", "<warehouse-executive-user-id>"]
}
```

Assigned users can receive stock, create batches, and move stock in that warehouse. `SUPER_ADMIN` and `PLANT_MANAGER` retain cross-warehouse operational access.

## Batches and stock movements

### `GET /batches`

Authenticated. Returns all inventory batches.

### `POST /batches`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `WAREHOUSE_EXECUTIVE`.

```json
{
  "batchNumber": "PP-20260815-001",
  "materialId": "<material-id>",
  "supplierLot": "SUP-LOT-91",
  "warehouseId": "<warehouse-id>",
  "unitId": "UNIT-1",
  "quantity": 2500,
  "uom": "KG",
  "expiresAt": "2027-08-15T00:00:00Z",
  "barcode": "PP-20260815-001"
}
```

Returns `201 Created`. New batches begin in `QUARANTINE`, and a `RECEIPT` movement is recorded automatically.

### `POST /batches/{id}/release`

Roles: `SUPER_ADMIN`, `QUALITY_MANAGER`.

Releases a cleared batch from `QUARANTINE` to `AVAILABLE`. A `REJECTED` batch cannot be released.

### `POST /movements`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `PRODUCTION_MANAGER`, `WAREHOUSE_EXECUTIVE`, `DISPATCH_EXECUTIVE`.

```json
{
  "batchId": "<batch-id>",
  "type": "CONSUMPTION",
  "quantity": 125,
  "reference": "PROD-RUN-20260815-01"
}
```

Allowed types:

| Type | Effect |
|---|---|
| `CONSUMPTION` | Deducts material from the batch. |
| `SCRAP` | Deducts scrap/rejected production quantity. |
| `DISPATCH` | Deducts dispatched finished goods. |
| `ADJUSTMENT` | Adds a positive adjustment quantity. |
| `PRODUCTION_OUTPUT` | Adds production output quantity. |

`RECEIPT`, `TRANSFER_IN`, and `TRANSFER_OUT` cannot be submitted here; use the dedicated batch or transfer workflows. A quantity greater than available stock returns `409 INSUFFICIENT_STOCK`.

## Warehouse transfers

### `GET /transfers`

Authenticated. Returns all transfer records.

### `POST /transfers`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `WAREHOUSE_EXECUTIVE`.

```json
{
  "batchId": "<batch-id>",
  "fromWarehouseId": "<source-warehouse-id>",
  "toWarehouseId": "<destination-warehouse-id>",
  "quantity": 500
}
```

Returns `201 Created` with status `PENDING` and an auto-generated `transferNumber` such as `TRF-1234ABCD`.

### `POST /transfers/{id}/approve`

Roles: `SUPER_ADMIN`, `PLANT_MANAGER`, `STORE_MANAGER`.

Approves a pending transfer and changes status to `APPROVED`.

### `POST /transfers/{id}/receive`

Roles: `SUPER_ADMIN`, `STORE_MANAGER`, `WAREHOUSE_EXECUTIVE`.

Receives an approved transfer and changes status to `RECEIVED`.

Transfer states: `PENDING` -> `APPROVED` -> `RECEIVED`. `REJECTED` is reserved for the forthcoming rejection workflow.

## Quality inspections

### `GET /quality-inspections`

Authenticated. Returns all inspection records.

### `POST /quality-inspections`

Roles: `SUPER_ADMIN`, `QUALITY_MANAGER`.

```json
{
  "batchId": "<batch-id>",
  "stage": "INCOMING",
  "result": "PASSED",
  "tests": [
    {
      "name": "MFI",
      "value": "12.4",
      "unit": "g/10 min",
      "withinSpec": true
    },
    {
      "name": "Colour Match",
      "value": "Pass",
      "withinSpec": true
    }
  ],
  "notes": "Incoming lot conforms to the purchase specification."
}
```

`result` accepts `PENDING`, `PASSED`, or `FAILED`. A failed inspection sets its batch status to `REJECTED`.

## Dashboard

### `GET /dashboard/summary`

Authenticated. Returns consolidated available-stock quantity, low-stock materials, and quarantine-batch count.

```json
{
  "availableStock": { "quantity": 2500, "batches": 1 },
  "lowStock": [],
  "quarantineBatches": 0
}
```

## Postman setup

1. Create variables: `baseUrl = http://localhost:4000/api/v1` and `token`.
2. Call `POST {{baseUrl}}/auth/login`.
3. In Tests, save the token:

```javascript
pm.environment.set("token", pm.response.json().accessToken);
```

4. Set Authorization to Bearer Token with `{{token}}` for every protected request.

## Products and finished goods

`GET /products` lists finished-good product masters. `POST /products` and `PUT /products/{id}` are available to `SUPER_ADMIN`, `STORE_MANAGER`, and `PRODUCTION_MANAGER`.

```json
{"sku":"FG-PIPE-001","name":"PVC Pipe 1 inch","category":"FINISHED_GOOD","unit":"PCS","reorderPoint":100}
```

Finished-goods batches are created automatically when a production run is completed.

## Production runs

`GET /production-runs` lists runs. `POST /production-runs` and `POST /production-runs/{id}/complete` require `SUPER_ADMIN` or `PRODUCTION_MANAGER`.

Start a run (available raw-material batches are consumed immediately):

```json
{"productId":"<product-id>","unitId":"UNIT-1","warehouseId":"<warehouse-id>","consumptions":[{"batchId":"<raw-batch-id>","quantity":100}]}
```

Complete it (creates an available finished-goods batch):

```json
{"outputQuantity":95,"outputBatchNumber":"FG-20260815-001","outputUom":"PCS"}
```

## Suppliers and purchase orders

`GET /suppliers`, `POST /suppliers`, and `PUT /suppliers/{id}` manage suppliers; writes require `SUPER_ADMIN` or `PURCHASE_MANAGER`.

```json
{"name":"ABC Polymers","contactName":"Ravi","email":"orders@abc.com","phone":"9876543210","leadTimeDays":7}
```

`GET /purchase-orders`, `POST /purchase-orders`, and `PATCH /purchase-orders/{id}/status` manage procurement.

```json
{"supplierId":"<supplier-id>","lines":[{"materialId":"<material-id>","quantity":1000,"unitPrice":82.5}]}
```

Order statuses: `DRAFT`, `APPROVED`, `ORDERED`, `PART_RECEIVED`, `RECEIVED`, `CANCELLED`.

`GET /reorder-recommendations` returns low-stock materials with suggested quantities and `LOW_STOCK` alerts.

## Barcode and QR operations

`POST /batches/{id}/label` generates a persistent code when needed and returns a QR payload, for example `stockai://batch/<batch-id>`. Requires `SUPER_ADMIN`, `STORE_MANAGER`, or `WAREHOUSE_EXECUTIVE`.

`GET /scan/{code}` resolves a scanned barcode/QR label to its batch details. It is authenticated.

## User administration

Super-admin only:

- `GET /users` - list users and their warehouse assignments.
- `POST /users` - create a staff user with role, unit, password, and optional warehouse IDs.
- `PUT /users/{id}` - update a user's name, role, unit, and warehouse assignments.
- `PATCH /users/{id}/active?value=false` - deactivate/activate a user.
