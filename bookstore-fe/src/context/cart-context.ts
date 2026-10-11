import { createContext } from 'react';
import type { Book, CartItem } from '../types/bookstore';
import type { OrderResponse } from '../types/order';

export interface CartState {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
  isLoading: boolean;
  error: string | null;
}

export interface CartContextValue extends CartState {
  addToCart: (book: Book) => Promise<void>;
  updateQuantity: (bookId: string, quantity: number) => Promise<void>;
  removeFromCart: (bookId: string) => Promise<void>;
  checkout: (idempotencyKey: string) => Promise<OrderResponse | null>;
}

export const CartContext = createContext<CartContextValue | undefined>(
  undefined,
);
