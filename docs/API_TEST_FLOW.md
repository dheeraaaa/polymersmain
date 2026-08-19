# StockAI API execution and test flow

## One-click automated test

From the repository root, run `./postman/run-e2e.ps1`. It builds and starts the Docker backend, waits for its health endpoint, then runs the complete E2E Postman collection with assertions. It requires Docker Desktop, Node.js, and a valid `backend/.env` with the Atlas connection settings. Use `./postman/run-e2e.ps1 -SkipBuild` to reuse the current image.

This playbook runs the currently implemented API in a dependency-safe order. It is designed for Postman, but each request works in any HTTP client.

## 1. Prerequisites

Before starting:

1. Start the API from the repository root:

   ```powershell
   docker compose up -d --build
   ```

2. Confirm the API is healthy:

   ```http
   GET http://localhost:4000/api/v1/health
   ```

3. Use a fresh Atlas database for a clean full-flow test. If a `SUPER_ADMIN` already exists, `/auth/register` is intentionally closed; log in as that administrator instead.

4. Postman environment variables:

   | Variable | Initial value | Set by |
   |---|---|---|
   | `baseUrl` | `http://localhost:4000/api/v1` | Manual |
   | `adminToken` | blank | Step 2 |
   | `storeToken` | blank | Step 5 |
   | `warehouseToken` | blank | Step 5 |
   | `productionToken` | blank | Step 5 |
   | `purchaseToken` | blank | Step 5 |
   | `qualityToken` | blank | Step 5 |
   | `warehouseId` | blank | Step 3 |
   | `materialId` | blank | Step 6 |
   | `productId` | blank | Step 7 |
   | `supplierId` | blank | Step 8 |
   | `rawBatchId` | blank | Step 10 |
   | `finishedBatchId` | blank | Step 13 |
   | `productionRunId` | blank | Step 12 |
   | `transferId` | blank | Step 15 |

For protected requests, set Authorization to **Bearer Token** and provide the relevant token variable, for example `{{adminToken}}`.

## 2. Bootstrap administrator

### `POST {{baseUrl}}/auth/register`

This works only when no super administrator exists and always creates `SUPER_ADMIN`. Do not send a `role` field.

```json
{
  "name": "System Administrator",
  "email": "admin@stockai.test",
  "password": "AdminPass-2026-Strong",
  "unitId": "UNIT-1"
}
```

Expected: `201 Created`, with `accessToken` and `user.id`.

Postman Tests:

```javascript
pm.test("Administrator created", () => pm.response.to.have.status(201));
pm.environment.set("adminToken", pm.response.json().accessToken);
pm.environment.set("adminUserId", pm.response.json().user.id);
```

Dependency result: `adminToken` authorizes all master-data setup requests.

If registration returns `REGISTRATION_CLOSED`, use `POST /auth/login` with an existing super-admin account and save `accessToken` as `adminToken`.

## 3. Create warehouse

### `POST {{baseUrl}}/warehouses`

Privilege: `SUPER_ADMIN`, `PLANT_MANAGER`, or `STORE_MANAGER`.

Token: `{{adminToken}}`.

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

Expected: `201 Created` with `_id` serialized as `id`, plus a readable `code` such as `WH-A1B2C3D4`.

```javascript
pm.test("Warehouse created", () => pm.response.to.have.status(201));
pm.environment.set("warehouseId", pm.response.json().id);
pm.environment.set("warehouseCode", pm.response.json().code);
```

Use `warehouseId` in all later API requests. Do **not** use `warehouseCode` where an ID is required.

## 4. Create operational users

All staff users are created by the super administrator.

### `POST {{baseUrl}}/users`

Privilege: `SUPER_ADMIN`.

Token: `{{adminToken}}`.

Create these five users. Each request returns `201 Created` and an `id`; save it using the test script below.

| User | Role | Why needed |
|---|---|---|
| Store user | `STORE_MANAGER` | Material, batch, transfer, warehouse operations |
| Warehouse user | `WAREHOUSE_EXECUTIVE` | Batch, label, scan, transfer receipt |
| Production user | `PRODUCTION_MANAGER` | Products and production runs |
| Purchase user | `PURCHASE_MANAGER` | Suppliers and purchase orders |
| Quality user | `QUALITY_MANAGER` | QC inspection and batch release |

Example - store user:

```json
{
  "name": "Store Manager",
  "email": "store@stockai.test",
  "password": "StorePass-2026-Strong",
  "role": "STORE_MANAGER",
  "unitId": "UNIT-1",
  "warehouseIds": ["{{warehouseId}}"]
}
```

Save each ID by changing the variable name in this script:

```javascript
pm.test("User created", () => pm.response.to.have.status(201));
pm.environment.set("storeUserId", pm.response.json().id);
```

For warehouse and production users, include `"warehouseIds": ["{{warehouseId}}"]`. The purchase and quality users do not require a warehouse assignment for their current APIs.

## 5. Log in each operational user

### `POST {{baseUrl}}/auth/login`

```json
{
  "email": "store@stockai.test",
  "password": "StorePass-2026-Strong"
}
```

Expected: `200 OK` and `accessToken`.

Repeat for each role. Save tokens separately:

```javascript
pm.test("Login succeeded", () => pm.response.to.have.status(200));
pm.environment.set("storeToken", pm.response.json().accessToken);
```

Use `warehouseToken`, `productionToken`, `purchaseToken`, and `qualityToken` for their matching users.

## 6. Create raw material

### `POST {{baseUrl}}/materials`

Privilege: `SUPER_ADMIN`, `STORE_MANAGER`, or `PURCHASE_MANAGER`.

Token: `{{storeToken}}`.

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

```javascript
pm.test("Material created", () => pm.response.to.have.status(201));
pm.environment.set("materialId", pm.response.json().id);
```

Dependency result: `materialId` is required for a raw-material batch and purchase-order line.

## 7. Create finished-goods product

### `POST {{baseUrl}}/products`

Privilege: `SUPER_ADMIN`, `STORE_MANAGER`, or `PRODUCTION_MANAGER`.

Token: `{{productionToken}}`.

```json
{
  "sku": "FG-PIPE-001",
  "name": "PVC Pipe 1 inch",
  "category": "FINISHED_GOOD",
  "unit": "PCS",
  "reorderPoint": 100
}
```

```javascript
pm.test("Product created", () => pm.response.to.have.status(201));
pm.environment.set("productId", pm.response.json().id);
```

Dependency result: `productId` is required to start production.

## 8. Create supplier

### `POST {{baseUrl}}/suppliers`

Privilege: `SUPER_ADMIN` or `PURCHASE_MANAGER`.

Token: `{{purchaseToken}}`.

```json
{
  "name": "ABC Polymers",
  "contactName": "Ravi Kumar",
  "email": "orders@abcpolymers.test",
  "phone": "9876543210",
  "leadTimeDays": 7
}
```

```javascript
pm.test("Supplier created", () => pm.response.to.have.status(201));
pm.environment.set("supplierId", pm.response.json().id);
```

## 9. Create purchase order

### `POST {{baseUrl}}/purchase-orders`

Privilege: `SUPER_ADMIN` or `PURCHASE_MANAGER`.

Token: `{{purchaseToken}}`.

```json
{
  "supplierId": "{{supplierId}}",
  "lines": [
    {
      "materialId": "{{materialId}}",
      "quantity": 2500,
      "unitPrice": 82.5
    }
  ]
}
```

Expected: `201 Created`, status `DRAFT`. Save `id` as `purchaseOrderId`.

Optional state test:

### `PATCH {{baseUrl}}/purchase-orders/{{purchaseOrderId}}/status`

```json
{ "status": "ORDERED" }
```

## 10. Receive raw-material batch

### `POST {{baseUrl}}/batches`

Privilege: `SUPER_ADMIN`, `STORE_MANAGER`, or `WAREHOUSE_EXECUTIVE`.

Token: `{{warehouseToken}}` or `{{storeToken}}`.

The authenticated user must be assigned to `warehouseId`.

```json
{
  "batchNumber": "PP-20260815-001",
  "materialId": "{{materialId}}",
  "supplierLot": "ABC-LOT-001",
  "warehouseId": "{{warehouseId}}",
  "unitId": "UNIT-1",
  "quantity": 2500,
  "uom": "KG"
}
```

Expected: `201 Created`, status `QUARANTINE`.

```javascript
pm.test("Raw material batch received", () => pm.response.to.have.status(201));
pm.environment.set("rawBatchId", pm.response.json().id);
```

## 11. Test QR/barcode label and QC release

### Generate label: `POST {{baseUrl}}/batches/{{rawBatchId}}/label`

Privilege: `SUPER_ADMIN`, `STORE_MANAGER`, or `WAREHOUSE_EXECUTIVE`.

Token: `{{warehouseToken}}`.

Expected response:

```json
{
  "format": "QR",
  "code": "BCH-...",
  "payload": "stockai://batch/<batch-id>",
  "batchId": "<batch-id>"
}
```

```javascript
pm.environment.set("barcode", pm.response.json().code);
```

### Scan label: `GET {{baseUrl}}/scan/{{barcode}}`

Authenticated. Token: `{{warehouseToken}}`.

Expected: the same raw batch, including warehouse, quantity, status, and material ID.

### Record QC: `POST {{baseUrl}}/quality-inspections`

Privilege: `SUPER_ADMIN` or `QUALITY_MANAGER`.

Token: `{{qualityToken}}`.

```json
{
  "batchId": "{{rawBatchId}}",
  "stage": "INCOMING",
  "result": "PASSED",
  "tests": [
    { "name": "MFI", "value": "12.4", "unit": "g/10 min", "withinSpec": true }
  ],
  "notes": "Material accepted"
}
```

### Release batch: `POST {{baseUrl}}/batches/{{rawBatchId}}/release`

Privilege: `SUPER_ADMIN` or `QUALITY_MANAGER`.

Token: `{{qualityToken}}`.

Expected: batch status changes from `QUARANTINE` to `AVAILABLE`.

## 12. Start production run

### `POST {{baseUrl}}/production-runs`

Privilege: `SUPER_ADMIN` or `PRODUCTION_MANAGER`.

Token: `{{productionToken}}`.

The raw batch must be `AVAILABLE`; production consumes the stated material quantity immediately.

```json
{
  "productId": "{{productId}}",
  "unitId": "UNIT-1",
  "warehouseId": "{{warehouseId}}",
  "consumptions": [
    { "batchId": "{{rawBatchId}}", "quantity": 100 }
  ]
}
```

```javascript
pm.test("Production run started", () => pm.response.to.have.status(201));
pm.environment.set("productionRunId", pm.response.json().id);
```

## 13. Complete production run and create finished goods

### `POST {{baseUrl}}/production-runs/{{productionRunId}}/complete`

Privilege: `SUPER_ADMIN` or `PRODUCTION_MANAGER`.

Token: `{{productionToken}}`.

```json
{
  "outputQuantity": 95,
  "outputBatchNumber": "FG-20260815-001",
  "outputUom": "PCS"
}
```

Expected: run status `COMPLETED`; `outputBatchId` is the available finished-goods batch.

```javascript
pm.test("Production completed", () => pm.response.to.have.status(200));
pm.environment.set("finishedBatchId", pm.response.json().outputBatchId);
```

Generate/scan a label for `finishedBatchId` using the same Step 11 endpoints to verify finished-goods traceability.

## 14. Reorder alerts and dashboard

### `GET {{baseUrl}}/reorder-recommendations`

Authenticated. Token: `{{purchaseToken}}`.

Returns raw materials whose available quantity is at/below the reorder point, with `suggestedQuantity` and `LOW_STOCK` alert data.

### `GET {{baseUrl}}/dashboard/summary`

Authenticated. Token: any logged-in user.

Returns available stock summary, low-stock material records, and quarantine batch count.

## 15. Transfer workflow

Create a second warehouse first, using Step 3, and save its ID as `destinationWarehouseId`. Assign the store/warehouse user to it using:

### `PUT {{baseUrl}}/warehouses/{{destinationWarehouseId}}/staff`

```json
{ "userIds": ["{{storeUserId}}", "{{warehouseUserId}}"] }
```

This endpoint supports either a full or partial transfer. For a partial transfer, the source batch retains its remaining quantity and the backend creates a new destination batch. Use a quantity no greater than the available balance.

### Create: `POST {{baseUrl}}/transfers`

Token: `{{storeToken}}`.

```json
{
  "batchId": "{{finishedBatchId}}",
  "fromWarehouseId": "{{warehouseId}}",
  "toWarehouseId": "{{destinationWarehouseId}}",
  "quantity": 50
}
```

Save `id` as `transferId`.

### Approve: `POST {{baseUrl}}/transfers/{{transferId}}/approve`

Token: `{{storeToken}}` or `{{adminToken}}`.

### Receive: `POST {{baseUrl}}/transfers/{{transferId}}/receive`

Token: an assigned destination warehouse/store user.

Expected: status `RECEIVED`. For a full transfer, the original finished batch moves to `destinationWarehouseId`. For a partial transfer, the response contains `destinationBatchId` for the newly created destination batch.

## 16. User administration test

Use `{{adminToken}}`.

| Request | Purpose |
|---|---|
| `GET /users` | List users and confirm roles/warehouse IDs. |
| `PUT /users/{id}` | Update user name, role, unit, and full warehouse assignment list. |
| `PATCH /users/{id}/active?value=false` | Deactivate a user; their subsequent login fails. |
| `PATCH /users/{id}/active?value=true` | Restore account access. |

## Privilege quick reference

| Capability | Roles |
|---|---|
| Bootstrap first super administrator | Public, only if no super admin exists |
| Manage users | `SUPER_ADMIN` |
| Materials | `SUPER_ADMIN`, `STORE_MANAGER`, `PURCHASE_MANAGER` |
| Warehouses/staff | `SUPER_ADMIN`, `PLANT_MANAGER`, `STORE_MANAGER` |
| Receive batches/labels | `SUPER_ADMIN`, `STORE_MANAGER`, `WAREHOUSE_EXECUTIVE` plus warehouse assignment |
| Quality and release | `SUPER_ADMIN`, `QUALITY_MANAGER` |
| Products and production | `SUPER_ADMIN`, `STORE_MANAGER`/`PRODUCTION_MANAGER`; run actions require production manager/admin |
| Suppliers and purchase orders | `SUPER_ADMIN`, `PURCHASE_MANAGER` |
| Transfers | Store/warehouse role; approval also permits plant manager |

## Dependency map

```text
Super admin token
  -> warehouse ID
  -> staff user IDs + staff tokens
  -> material ID -> supplier ID -> purchase order ID
  -> raw batch ID -> QR label/scan -> QC -> available raw batch
  -> product ID + available raw batch -> production run ID -> finished batch ID
  -> finished batch ID + second warehouse ID -> transfer ID
```

If a request fails, first verify its token role, required IDs, warehouse assignment, source-batch status, and request sequence against this document.
