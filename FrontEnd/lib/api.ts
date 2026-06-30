import type {
  User,
  LoginRequest,
  RegisterRequest,
  CreateOrderRequest,
  CreateOrderItemRequest,
  CreateBookingRequest,
  BookingAvailabilityRequest,
  CreatePaymentRequest,
  CreatePaymentLinkRequest,
  CreatePaymentLinkResponse,
  PaymentLinkResponse,
  PaymentStatusResponse,
  AdminDashboardSummary,
  RecentOrder,
  MenuItem,
  Category,
  RestaurantTable,
  OrderItem,
  Order,
} from './types';

export type {
  MenuItem,
  Category,
  RestaurantTable,
  OrderItem,
  Order,
};
import { getApiBaseUrl } from './env';

async function fetchData<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const API_BASE_URL = getApiBaseUrl();
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  // Xử lý lỗi trước
  if (!response.ok) {
    let errMsg = response.statusText;

    try {
      const errorBody = await response.json();
      errMsg = errorBody.message || errorBody.error || errorBody.toString();
    } catch (_) {
      // Ignore
    }

    throw new Error(errMsg);
  }

  // Server có thể không trả JSON (ví dụ DELETE 204)
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json();
  }

  // @ts-ignore
  return null; // hoặc return undefined
}

export async function login(loginRequest: LoginRequest) {
  return fetchData<{ message: string; user: User; token?: string }>("/api/users/login", {
    method: 'POST',
    body: JSON.stringify(loginRequest),
  });
}

export async function register(registerRequest: RegisterRequest) {
  return fetchData<{ message: string; user: User; token?: string }>("/api/users/register", {
    method: 'POST',
    body: JSON.stringify(registerRequest),
  });
}

export async function getUsersList() {
  const users = await fetchData<User[]>("/api/users/list");
  return { message: "Users retrieved successfully", users };
}

export async function getUserById(id: number) {
  return fetchData<{ message: string; user: User }>(`/api/users/${id}`);
}

export async function updateUser(id: number, user: Partial<User>) {
  return fetchData<{ message: string; user: User }>(`/api/users/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(user),
  });
}

export async function deleteUser(id: number) {
  return fetchData<{ message: string }>(`/api/users/delete/${id}`, {
    method: 'DELETE',
  });
}

export async function updateProfile(id: number, profile: Partial<User>) {
  return fetchData<{ message: string; user: User }>(`/api/users/profile/${id}`, {
    method: 'PUT',
    body: JSON.stringify(profile),
  });
}

export async function changePassword(id: number, passwords: { currentPassword: string; newPassword: string; confirmPassword: string }) {
  return fetchData<{ message: string }>(`/api/users/change-password/${id}`, {
    method: 'PUT',
    body: JSON.stringify(passwords),
  });
}

export async function forgotPassword(email: string) {
  return fetchData<{ message: string }>(`/api/users/forgot-password`, {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export async function resetPassword(token: string, newPassword: string, confirmPassword: string) {
  return fetchData<{ message: string }>(`/api/users/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ token, newPassword, confirmPassword }),
  });
}

export async function validateResetToken(token: string) {
  return fetchData<{ valid: boolean; message: string }>(`/api/users/validate-reset-token?token=${token}`);
}

// ===== MENU ITEMS API =====

export async function getMenuItems() {
  return fetchData<{ message: string; menuItems: MenuItem[] }>("/api/menu-items/list");
}

export async function getMenuItemById(id: number) {
  return fetchData<{ message: string; menuItem: MenuItem }>(`/api/menu-items/${id}`);
}

export async function getMenuItemsByCategory(categoryId: number) {
  return fetchData<{ message: string; menuItems: MenuItem[] }>(`/api/menu-items/category/${categoryId}`);
}

export async function createMenuItem(menuItem: Omit<MenuItem, 'id'>) {
  return fetchData<{ message: string; menuItem: MenuItem }>("/api/menu-items/create", {
    method: 'POST',
    body: JSON.stringify(menuItem),
  });
}

export async function updateMenuItem(id: number, menuItem: Partial<MenuItem>) {
  return fetchData<{ message: string; menuItem: MenuItem }>(`/api/menu-items/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(menuItem),
  });
}

export async function deleteMenuItem(id: number) {
  return fetchData<{ message: string }>(`/api/menu-items/delete/${id}`, {
    method: 'DELETE',
  });
}

// ===== CATEGORIES API =====

export async function getCategories() {
  return fetchData<{ message: string; categories: Category[] }>("/api/categories/list");
}

export async function createCategory(category: Omit<Category, 'id'>) {
  return fetchData<{ message: string; category: Category }>("/api/categories/create", {
    method: 'POST',
    body: JSON.stringify(category),
  });
}

export async function updateCategory(id: number, category: Partial<Category>) {
  return fetchData<{ message: string; category: Category }>(`/api/categories/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(category),
  });
}

export async function deleteCategory(id: number) {
  return fetchData<{ message: string }>(`/api/categories/delete/${id}`, {
    method: 'DELETE',
  });
}

// ===== TABLES API =====

export async function getTablesList() {
  return fetchData<{ message: string; tables: RestaurantTable[] }>("/api/tables/list");
}

export async function getAllTables() {
  return fetchData<{ message: string; tables: RestaurantTable[] }>("/api/tables/all");
}

export async function getAvailableTables() {
  return fetchData<{ message: string; tables: RestaurantTable[] }>("/api/tables/available");
}

export async function updateTableStatus(tableId: number, status: string) {
  return fetchData<{ message: string; table: RestaurantTable }>(`/api/tables/${tableId}/status-update/${status}`, {
    method: 'PUT',
  });
}

export async function checkInTable(qrCode: string, customerId: number) {
  return fetchData<{ message: string; table: RestaurantTable; order: Order }>(`/api/tables/checkin/${qrCode}?customerId=${customerId}`, {
    method: 'POST',
  });
}

export async function checkOutTable(tableId: number) {
  return fetchData<{ message: string; table: RestaurantTable }>(`/api/tables/${tableId}/checkout`, {
    method: 'POST',
  });
}

export async function getTableCurrentOrder(tableId: number) {
  return fetchData<{ message: string; hasActiveOrder: boolean; order?: Order }>(`/api/tables/${tableId}/current-order`);
}

export async function sendQRCodeToESP32(tableId: number) {
  return fetchData<{ message: string; tableId: number; tableName: string; qrCode: string; qrUrl: string }>(`/api/send-qr-code/${tableId}`, {
    method: 'POST',
  });
}

export function getQRCodeImageUrl(tableId: number) {
  return `${getApiBaseUrl()}/api/qr-code/${tableId}/image`;
}

export async function getTableByQr(qrCode: string) {
  return fetchData<{ message: string; table: RestaurantTable }>(`/api/tables/qr/${qrCode}`);
}

export async function createTable(table: { tableName: string; capacity: number; status: string; tableType?: string; location?: string }) {
  return fetchData<{ message: string; table: RestaurantTable }>("/api/tables/create", {
    method: 'POST',
    body: JSON.stringify(table),
  });
}

export async function updateTable(id: number, table: Partial<RestaurantTable>) {
  return fetchData<{ message: string; table: RestaurantTable }>(`/api/tables/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(table),
  });
}

export async function deleteTable(id: number) {
  return fetchData<{ message: string }>(`/api/tables/delete/${id}`, {
    method: 'DELETE',
  });
}


// ===== ORDERS API =====

export async function getOrders() {
  return fetchData<{ message: string; orders: Order[] }>("/api/orders/list");
}

export async function getMyOrders(customerId?: number) {
  const url = customerId 
    ? `/api/orders/my-orders?customerId=${customerId}`
    : "/api/orders/my-orders";
  return fetchData<{ message: string; orders: Order[] }>(url);
}

export async function getOrderById(id: number) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${id}`);
}

export async function getOrdersByTable(tableId: number) {
  return fetchData<{ message: string; orders: Order[] }>(`/api/orders/table/${tableId}`);
}

export async function getActiveOrdersByTable(tableId: number) {
  return fetchData<{ message: string; orders: Order[] }>(`/api/orders/table/${tableId}/active`);
}

export async function createOrder(order: Omit<Order, 'id' | 'createdAt' | 'updatedAt'>) {
  return fetchData<{ message: string; order: Order }>("/api/orders/create", {
    method: 'POST',
    body: JSON.stringify(order),
  });
}

export async function createOrderWithCustomer(customerId: number, tableId: number, order: Omit<Order, 'id' | 'customer' | 'table' | 'createdAt' | 'updatedAt'>) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/create-with-customer/${customerId}/table/${tableId}`, {
    method: 'POST',
    body: JSON.stringify(order),
  });
}

export async function createOrderFromRequest(request: { tableId: number; customerId?: number | null; staffId?: number; bookingId?: number | null; status?: string; items?: Omit<OrderItem,'id'>[] }) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/create-from-request`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function addItemsToOrder(orderId: number, items: Omit<OrderItem, 'id'>[]) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${orderId}/add-items`, {
    method: 'POST',
    body: JSON.stringify(items),
  });
}

export async function removeItemFromOrder(orderId: number, itemId: number) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${orderId}/remove-item/${itemId}`, {
    method: 'DELETE',
  });
}

export async function updateOrderStatus(id: number, status: string) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${id}/status/${status}`, {
    method: 'PUT',
  });
}

export async function checkoutOrder(orderId: number) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${orderId}/checkout`, {
    method: 'PUT',
  });
}

export async function updateOrderItemStatus(orderId: number, itemId: number, status: string) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${orderId}/item/${itemId}/status/${status}`, {
    method: 'PUT',
  });
}

export async function deleteOrder(id: number) {
  return fetchData<{ message: string }>(`/api/orders/${id}`, {
    method: 'DELETE',
  });
}

// ===== NEW ORDER FLOW API =====
export async function getOrCreateActiveOrder(tableId: number, customerId?: number) {
  const url = customerId 
    ? `/api/orders/table/${tableId}/get-or-create?customerId=${customerId}`
    : `/api/orders/table/${tableId}/get-or-create`;
  return fetchData<{ message: string; order: Order }>(url, {
    method: 'POST',
  });
}

export async function addItemsToTableOrder(tableId: number, items: Omit<OrderItem, 'id'>[], customerId?: number) {
  const url = customerId
    ? `/api/orders/table/${tableId}/add-items?customerId=${customerId}`
    : `/api/orders/table/${tableId}/add-items`;
  return fetchData<{ message: string; order: Order }>(url, {
    method: 'POST',
    body: JSON.stringify(items),
  });
}

export async function closeOrder(orderId: number) {
  return fetchData<{ message: string; order: Order }>(`/api/orders/${orderId}/close`, {
    method: 'PUT',
  });
}

// ===== BOOKINGS API =====
export interface Booking {
  id: number;
  customerId: number;
  tableId: number;
  date: string;
  time: string;
  guests: number;
  status: string;
  notes?: string;
}

export async function getBookings() {
  return fetchData<{ message: string; bookings: Booking[] }>("/api/bookings/list");
}

export async function getMyBookings(customerId?: number) {
  const url = customerId 
    ? `/api/bookings/my-bookings?customerId=${customerId}`
    : "/api/bookings/my-bookings";
  return fetchData<{ message: string; bookings: Booking[] }>(url);
}

export async function getActiveBookingByTable(tableId: number) {
  return fetchData<{ message: string; hasActiveBooking: boolean; booking?: Booking }>(`/api/bookings/table/${tableId}/active`);
}

export async function createBooking(booking: Omit<Booking, 'id'>) {
  return fetchData<{ message: string; booking: Booking }>("/api/bookings/create", {
    method: 'POST',
    body: JSON.stringify(booking),
  });
}

export async function confirmBooking(bookingId: number) {
  return fetchData<{ message: string; booking: Booking }>(`/api/bookings/${bookingId}/confirm`, {
    method: 'PUT',
  });
}

export async function checkInBooking(bookingId: number) {
  return fetchData<{ message: string; booking: Booking }>(`/api/bookings/${bookingId}/checkin`, {
    method: 'PUT',
  });
}

export async function checkTableAvailability(date: string, time: string, guests: number) {
  const params = new URLSearchParams({
    date,
    time,
    guests: guests.toString()
  });
  return fetchData<{
    message: string;
    date: string;
    time: string;
    guests: number;
    availableTables: RestaurantTable[]
  }>(`/api/bookings/availability?${params}`);
}

export async function cancelBooking(bookingId: number) {
  return fetchData<{ message: string; booking: Booking }>(`/api/bookings/${bookingId}/cancel`, {
    method: 'PUT',
  });
}

// ===== PAYMENTS API =====
export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  paymentMethod: string;
  status: string;
  transactionId?: string;
}

export async function processPayment(payment: Omit<Payment, 'id'>) {
  return fetchData<{ message: string; payment: Payment }>(`/api/payments/process/${payment.orderId}`, {
    method: 'POST',
    body: JSON.stringify(payment),
  });
}

// ===== SEPAY PAYMENT API =====
export interface SepayPaymentRequest {
  orderId: number;
  amount: number;
  description?: string;
}

export interface SepayPaymentResponse {
  success: boolean;
  message: string;
  data: {
    transactionId: string;
    orderId: number;
    amount: number;
    accountNumber: string;
    accountName: string;
    bankCode: string;
    content: string;
    paymentUrl: string; // QR code image URL
    status: string;
    expiresAt: string;
  };
}

export interface SepayPaymentStatus {
  success: boolean;
  message: string;
  data: {
    transactionId: string;
    orderId: number;
    amount: number;
    status: string; // PENDING, COMPLETED, FAILED, CANCELLED
    paidAt?: string;
    bankTransactionId?: string;
  };
}

export async function createSepayPayment(request: SepayPaymentRequest) {
  return fetchData<SepayPaymentResponse>("/api/payments/sepay/create", {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function checkSepayPaymentStatus(transactionId: string) {
  return fetchData<SepayPaymentStatus>(`/api/payments/sepay/status/${transactionId}`);
}

export async function cancelSepayPayment(transactionId: string) {
  return fetchData<{ success: boolean; message: string }>(`/api/payments/sepay/cancel/${transactionId}`, {
    method: 'POST',
  });
}

// ===== PAYOS PAYMENT API =====
export async function createPayOSPaymentLink(
  orderId: number,
  token: string
) {
  return fetchData<CreatePaymentLinkResponse>("/api/payos/link", {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ orderId }),
  });
}

export async function createPaymentLinkForOrders(orderIds: number[], token: string) {
  return fetchData<PaymentLinkResponse>("/api/payments/link", {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(orderIds),
  });
}

export async function getPaymentByOrderId(orderId: number, token: string) {
  return fetchData<PaymentStatusResponse>(`/api/payments/order/${orderId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

export async function getPaymentById(paymentId: number, token: string) {
  return fetchData<PaymentStatusResponse>(`/api/payments/${paymentId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}


// ===== ADMIN API =====

/**
 * Get JWT token from localStorage
 */
function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('token') || localStorage.getItem('jwt');
}

/**
 * Get dashboard summary statistics
 */
export async function getAdminDashboardSummary(): Promise<AdminDashboardSummary> {
  const token = getAuthToken();
  if (!token) {
    throw new Error('Authentication required');
  }
  
  return fetchData<AdminDashboardSummary>('/api/admin/dashboard/summary', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

/**
 * Get recent orders (latest 5 by default)
 */
export async function getRecentOrders(limit: number = 5): Promise<{ orders: RecentOrder[] }> {
  const token = getAuthToken();
  if (!token) {
    throw new Error('Authentication required');
  }
  
  return fetchData<{ orders: RecentOrder[] }>(`/api/admin/orders/recent?limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

/**
 * Get pending reservations
 */
export async function getPendingReservations(): Promise<{ reservations: Booking[] }> {
  const token = getAuthToken();
  if (!token) {
    throw new Error('Authentication required');
  }
  
  return fetchData<{ reservations: Booking[] }>('/api/admin/reservations/pending', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

/**
 * Approve a reservation
 */
export async function approveReservation(id: number): Promise<{ message: string; reservation: Booking }> {
  const token = getAuthToken();
  if (!token) {
    throw new Error('Authentication required');
  }
  
  return fetchData<{ message: string; reservation: Booking }>(`/api/admin/reservations/${id}/approve`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

/**
 * Reject a reservation
 */
export async function rejectReservation(id: number): Promise<{ message: string; reservation: Booking }> {
  const token = getAuthToken();
  if (!token) {
    throw new Error('Authentication required');
  }
  
  return fetchData<{ message: string; reservation: Booking }>(`/api/admin/reservations/${id}/reject`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}

