export type UserRole = 'seller' | 'accountant' | 'seller_sec';
export type PlatformKey = 'ml' | 'shopee' | 'amazon' | 'manual';
export type OrderStatus = 'paid' | 'pending' | 'cancelled';
export type NotificationType = 'success' | 'info' | 'warning' | 'danger';
export type AppSection = 'dashboard' | 'orders' | 'chat' | 'connectors';

export interface Tenant {
  id: string;
  name: string;
  sellerName: string;
  email: string;
  role: UserRole;
  plan: string;
  trialDaysLeft: number;
  mlConnected: boolean;
  shopeeConnected: boolean;
  amazonConnected: boolean;
}

export interface Connector {
  key: string;
  name: string;
  active: boolean;
  version: string;
  description: string;
  type: string;
}

export interface OrderItem {
  name: string;
  qty: number;
  unit_price: number;
}

export interface Order {
  order_id: string;
  platform: PlatformKey;
  date: string;
  gross_value: number;
  platform_fee: number;
  net_value: number;
  payment_method: string;
  payment_date: string;
  release_date: string;
  status: OrderStatus;
  buyer_name: string;
  items: OrderItem[];
  invoice_number: string;
}

export interface DashboardSummary {
  gross_value: number;
  platform_fee: number;
  net_value: number;
  pending_value: number;
  total_orders: number;
  paid_orders_count: number;
  average_ticket: number;
  platform_split: Record<string, number>;
  currency: string;
  active_integrations: string[];
}

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  date: string;
  read: boolean;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  date: Date;
  isReal?: boolean;
}

export interface ManualOrderPayload {
  buyer_name: string;
  item_name: string;
  qty: number;
  unit_price: number;
  platform_fee: number;
  payment_method: string;
  status: OrderStatus;
  invoice_number?: string;
}
