export interface OrderItem {
  id: number;
  bookId: number;
  title: string;
  author: string;
  isbn: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderResponse {
  id: number;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED' | string;
  items: OrderItem[];
  total: number;
  createdAt: string;
}
