export type Role = 'SUPER_ADMIN' | 'FACTORY_DIRECTOR' | 'PLANT_MANAGER' | 'STORE_MANAGER' | 'PURCHASE_MANAGER' | 'PRODUCTION_MANAGER' | 'QUALITY_MANAGER' | 'WAREHOUSE_EXECUTIVE' | 'DISPATCH_EXECUTIVE'
export type BatchStatus = 'QUARANTINE' | 'AVAILABLE' | 'CONSUMED' | 'EXPIRED' | 'REJECTED'
export type MovementType = 'RECEIPT' | 'CONSUMPTION' | 'PRODUCTION_OUTPUT' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'SCRAP' | 'ADJUSTMENT' | 'DISPATCH'
export type PurchaseOrderStatus = 'DRAFT' | 'APPROVED' | 'ORDERED' | 'PART_RECEIVED' | 'RECEIVED' | 'CANCELLED'
export type QualityResult = 'PENDING' | 'PASSED' | 'FAILED'
export type TransferStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'RECEIVED'

export interface User { id: string; name: string; email: string; role: Role; unitId?: string; warehouseIds?: string[]; active?: boolean }
export interface AuthResponse { accessToken: string; tokenType: string; user: User }
export interface Material { id: string; sku: string; name: string; category: string; unit: string; reorderPoint: number; leadTimeDays: number; active: boolean }
export interface Product { id: string; sku: string; name: string; category: string; unit: string; reorderPoint: number; active: boolean }
export interface Warehouse { id: string; code: string; name: string; unitId: string; type: string; address?: string; capacity: number; capacityUnit: string; active: boolean }
export interface Batch { id: string; batchNumber: string; materialId: string; supplierLot?: string; warehouseId: string; unitId: string; quantity: number; uom: string; receivedAt?: string; expiresAt?: string; status: BatchStatus; barcode?: string }
export interface Supplier { id: string; code: string; name: string; contactName?: string; email?: string; phone?: string; gstin?: string; leadTimeDays: number; active: boolean }
export interface PurchaseOrder { id: string; poNumber: string; supplierId: string; status: PurchaseOrderStatus; createdBy: string; lines: { materialId: string; quantity: number; unitPrice: number }[] }
export interface ProductionRun { id: string; runNumber: string; productId: string; unitId: string; warehouseId: string; status: string; consumptions: { batchId: string; quantity: number }[]; outputQuantity: number; outputBatchId?: string; startedAt?: string; completedAt?: string }
export interface Transfer { id: string; transferNumber: string; batchId: string; destinationBatchId?: string; fromWarehouseId: string; toWarehouseId: string; quantity: number; status: TransferStatus; requestedBy: string; approvedBy?: string }
export interface QualityInspection { id: string; batchId: string; stage: string; result: QualityResult; notes?: string; inspectedBy: string; tests: { name: string; value: string; unit?: string; withinSpec: boolean }[] }
export interface ReorderRecommendation { materialId: string; sku: string; name: string; availableQuantity: number; reorderPoint: number; suggestedQuantity: number; alert: 'LOW_STOCK' }
export interface DashboardSummary { availableStock: { quantity: number; batches: number }; lowStock: Material[]; quarantineBatches: number }
export interface Label { format: string; code: string; payload: string; batchId: string }
export interface ApiError { message: string; status?: number; details?: Record<string, string> }
