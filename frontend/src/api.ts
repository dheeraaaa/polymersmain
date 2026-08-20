import axios, { AxiosError } from 'axios'
import type { ApiError, AuthResponse, Batch, DashboardSummary, Label, Material, Product, ProductionRun, PurchaseOrder, PurchaseOrderStatus, QualityInspection, ReorderRecommendation, Supplier, Transfer, User, Warehouse } from './types'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:4000/api/v1', headers: { 'Content-Type': 'application/json' } })
client.interceptors.request.use((config) => { const token = localStorage.getItem('stockai_token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config })
client.interceptors.response.use((response) => response, (error: AxiosError<{ error?: { message?: string; details?: Record<string, string> } }>) => {
  if (error.response?.status === 401) window.dispatchEvent(new Event('stockai:unauthorized'))
  const status = error.response?.status
  const messages: Record<number, string> = { 400: 'Please check the information and try again.', 403: 'You do not have permission to do that.', 404: 'The requested record was not found.', 409: 'This action conflicts with the current record state.', 500: 'The server could not complete the request.' }
  const data = error.response?.data?.error
  return Promise.reject({ message: data?.message ?? messages[status ?? 0] ?? 'Cannot connect to the server. Check that it is running.', status, details: data?.details } satisfies ApiError)
})
const get = <T>(url: string) => client.get<T>(url).then((r) => r.data)
const post = <T>(url: string, body?: unknown) => client.post<T>(url, body).then((r) => r.data)
const put = <T>(url: string, body: unknown) => client.put<T>(url, body).then((r) => r.data)
const patch = <T>(url: string, body?: unknown) => client.patch<T>(url, body).then((r) => r.data)

export const authApi = {
  login: (body: { email: string; password: string }) => post<AuthResponse>('/auth/login', body),
  register: (body: { name: string; email: string; password: string; unitId?: string }) => post<AuthResponse>('/auth/register', body),
}
export const dashboardApi = { summary: () => get<DashboardSummary>('/dashboard/summary') }
function resourceApi<T>(path: string) { return { list: () => get<T[]>(path), create: (body: unknown) => post<T>(path, body), update: (id: string, body: unknown) => put<T>(`${path}/${id}`, body) } }
export const materialsApi = resourceApi<Material>('/materials')
export const productsApi = resourceApi<Product>('/products')
export const suppliersApi = resourceApi<Supplier>('/suppliers')
export const warehouseApi = { ...resourceApi<Warehouse>('/warehouses'), setActive: (id: string, value: boolean) => patch<Warehouse>(`/warehouses/${id}/active?value=${value}`), assignStaff: (id: string, userIds: string[]) => put<void>(`/warehouses/${id}/staff`, { userIds }) }
export const inventoryApi = { list: () => get<Batch[]>('/batches'), create: (body: unknown) => post<Batch>('/batches', body), release: (id: string) => post<Batch>(`/batches/${id}/release`), move: (body: unknown) => post<Batch>('/movements', body) }
export const productionApi = { list: () => get<ProductionRun[]>('/production-runs'), create: (body: unknown) => post<ProductionRun>('/production-runs', body), complete: (id: string, body: unknown) => post<ProductionRun>(`/production-runs/${id}/complete`, body) }
export const purchaseOrderApi = { list: () => get<PurchaseOrder[]>('/purchase-orders'), create: (body: unknown) => post<PurchaseOrder>('/purchase-orders', body), status: (id: string, status: PurchaseOrderStatus) => patch<PurchaseOrder>(`/purchase-orders/${id}/status`, { status }) }
export const transferApi = { list: () => get<Transfer[]>('/transfers'), create: (body: unknown) => post<Transfer>('/transfers', body), approve: (id: string) => post<Transfer>(`/transfers/${id}/approve`), receive: (id: string) => post<Transfer>(`/transfers/${id}/receive`) }
export const qualityApi = { list: () => get<QualityInspection[]>('/quality-inspections'), create: (body: unknown) => post<QualityInspection>('/quality-inspections', body) }
export const reorderApi = { list: () => get<ReorderRecommendation[]>('/reorder-recommendations') }
export const barcodeApi = { scan: (code: string) => get<Batch>(`/scan/${encodeURIComponent(code)}`), label: (id: string) => post<Label>(`/batches/${id}/label`) }
export const userApi = { list: () => get<User[]>('/users'), create: (body: unknown) => post<User>('/users', body), update: (id: string, body: unknown) => put<User>(`/users/${id}`, body), setActive: (id: string, value: boolean) => patch<User>(`/users/${id}/active?value=${value}`) }
