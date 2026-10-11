import { createContext } from 'react';
import type { Book, CartItem } from '../types/bookstore';

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
}

export const CartContext = createContext<CartContextValue | undefined>(
  undefined,
);
