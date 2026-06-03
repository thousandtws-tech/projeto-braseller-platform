export interface User {
  id: string;
  tenantId?: string;
  email: string;
  fullName?: string;
  preferredUsername?: string;
  firstName?: string;
  lastName?: string;
  pictureUrl?: string;
  emailVerified?: boolean;
  provider?: string;
  providerSubject?: string;
  status?: string;
  roles: string[];
}

export interface Tenant {
  id: string;
  name: string;
  sellerName: string;
  email: string;
  plan: string;
  trialDaysLeft: number;
  role: 'seller' | 'accountant' | 'seller_sec';
  mlConnected: boolean;
  shopeeConnected: boolean;
  amazonConnected: boolean;
}

export interface UserView {
  id: string;
  tenantId: string;
  email: string;
  fullName?: string;
  preferredUsername?: string;
  firstName?: string;
  lastName?: string;
  pictureUrl?: string;
  emailVerified?: boolean;
  provider?: string;
  providerSubject?: string;
  status?: string;
  roles: string[];
}

export interface AccountantAccessView {
  id: string;
  tenantId: string;
  accountantUserId: string;
  email: string;
  readOnly: boolean;
  status: string;
}

export interface GrantAccountantAccessRequest {
  email: string;
  fullName: string;
  temporaryPassword: string;
}

export interface OrderItem {
  name: string;
  qty: number;
  unitPrice: number;
}

export interface Order {
  id: string;
  platform: 'ml' | 'shopee' | 'amazon' | 'manual';
  date: string;
  grossValue: number;
  platformFee: number;
  netValue: number;
  paymentMethod: string;
  paymentDate: string;
  releaseDate: string;
  status: 'paid' | 'pending' | 'cancelled';
  buyerName: string;
  items: OrderItem[];
  invoiceNumber: string | null;
}

/**
 * Data Transfer Objects (DTO)
 * Representam o payload bruto enviado ou retornado pelo backend REST.
 * Serve como barreira anticorrosão para mudanças repentinas de API.
 */
export interface ApiOrderDto {
  order_id: string;
  platform: string;
  date_created: string;
  gross_value_cents: number; // API retorna em centavos por segurança corporativa
  platform_fee_cents: number;
  net_value_cents: number;
  payment_method_type: string;
  paid_at: string;
  release_at: string;
  order_status: string;
  buyer_username: string;
  purchased_items: {
    item_title: string;
    quantity: number;
    price_cents: number;
  }[];
  fiscal_invoice?: string;
}

/**
 * Mappers Arquiteturais (Anti-Corruption Translators)
 * Converte DTOs brutos de backend nos modelos de negócios tipados do Angular Core.
 */
export class OrderMapper {
  static fromDto(dto: ApiOrderDto): Order {
    return {
      id: dto.order_id,
      platform: (dto.platform === 'ml' || dto.platform === 'shopee' || dto.platform === 'amazon' || dto.platform === 'manual') 
        ? dto.platform 
        : 'manual',
      date: dto.date_created,
      grossValue: dto.gross_value_cents / 100, // Conversão de centavos para reais reais (BRL)
      platformFee: dto.platform_fee_cents / 100,
      netValue: dto.net_value_cents / 100,
      paymentMethod: dto.payment_method_type,
      paymentDate: dto.paid_at,
      releaseDate: dto.release_at,
      status: mapStatus(dto.order_status),
      buyerName: dto.buyer_username,
      items: (dto.purchased_items || []).map(item => ({
        name: item.item_title,
        qty: item.quantity,
        unitPrice: item.price_cents / 100
      })),
      invoiceNumber: dto.fiscal_invoice || null
    };
  }

  static toDto(model: Order): ApiOrderDto {
    return {
      order_id: model.id,
      platform: model.platform,
      date_created: model.date,
      gross_value_cents: Math.round(model.grossValue * 100),
      platform_fee_cents: Math.round(model.platformFee * 100),
      net_value_cents: Math.round(model.netValue * 100),
      payment_method_type: model.paymentMethod,
      paid_at: model.paymentDate,
      release_at: model.releaseDate,
      order_status: model.status,
      buyer_username: model.buyerName,
      purchased_items: model.items.map(item => ({
        item_title: item.name,
        quantity: item.qty,
        price_cents: Math.round(item.unitPrice * 100)
      })),
      fiscal_invoice: model.invoiceNumber || undefined
    };
  }
}

function mapStatus(status: string): 'paid' | 'pending' | 'cancelled' {
  switch (status.toLowerCase()) {
    case 'paid':
    case 'completed':
    case 'payment_done':
      return 'paid';
    case 'pending':
    case 'waiting_payment':
      return 'pending';
    default:
      return 'cancelled';
  }
}
