import {
  useCallback,
  useEffect,
  useReducer,
  useRef,
  type ReactNode,
} from 'react';
import type { Book, CartItem } from '../types/bookstore';
import type { OrderResponse } from '../types/order';
import { CartContext, type CartState } from './cart-context';

const CART_API_URL = 'http://localhost:8080/api/cart';
const CHECKOUT_API_URL = 'http://localhost:8080/api/orders/checkout';

interface ApiCartItem {
  id: number;
  bookId: number;
  title: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

interface ApiCart {
  id: number;
  items: ApiCartItem[];
  total: number;
}

type CartAction =
  | { type: 'set-items'; items: CartItem[] }
  | { type: 'set-response'; response: ApiCart }
  | { type: 'set-loading'; isLoading: boolean }
  | { type: 'set-error'; error: string | null };

const initialState: CartState = {
  items: [],
  totalItems: 0,
  totalPrice: 0,
  isLoading: true,
  error: null,
};

function calculateState(items: CartItem[], isLoading: boolean, error: string | null): CartState {
  return {
    items,
    totalItems: items.reduce((total, item) => total + item.quantity, 0),
    totalPrice: items.reduce(
      (total, item) => total + item.book.price * item.quantity,
      0,
    ),
    isLoading,
    error,
  };
}

function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case 'set-items':
      return calculateState(action.items, state.isLoading, state.error);
    case 'set-response': {
      const items = action.response.items.map((item) => {
        const previous = state.items.find(
          (cartItem) => cartItem.book.id === String(item.bookId),
        );
        const book: Book = previous
          ? {
              ...previous.book,
              title: item.title,
              price: item.unitPrice,
            }
          : {
              id: String(item.bookId),
              title: item.title,
              author: '',
              price: item.unitPrice,
              stock: 0,
            };
        return { id: String(item.id), book, quantity: item.quantity };
      });
      return calculateState(items, false, null);
    }
    case 'set-loading':
      return { ...state, isLoading: action.isLoading };
    case 'set-error':
      return { ...state, error: action.error };
  }
}

function getBookId(bookId: string): number {
  const id = Number(bookId);
  if (!Number.isSafeInteger(id) || id < 1) {
    throw new Error('This book cannot be added to the cart because its ID is invalid.');
  }
  return id;
}

async function readCartResponse(response: Response): Promise<ApiCart> {
  if (!response.ok) {
    let detail = '';
    try {
      const body: unknown = await response.json();
      if (
        typeof body === 'object' &&
        body !== null &&
        'message' in body &&
        typeof body.message === 'string'
      ) {
        detail = ` ${body.message}`;
      }
    } catch {
      // Some gateway errors return a non-JSON response.
    }
    throw new Error(`Cart request failed.${detail}`);
  }
  return (await response.json()) as ApiCart;
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(cartReducer, initialState);
  const stateRef = useRef(state);

  const send = useCallback((action: CartAction) => {
    stateRef.current = cartReducer(stateRef.current, action);
    dispatch(action);
  }, []);

  const applyResponse = useCallback((response: ApiCart) => {
    send({ type: 'set-response', response });
  }, [send]);

  useEffect(() => {
    let active = true;
    void fetch(CART_API_URL, { credentials: 'include' })
      .then(readCartResponse)
      .then((response) => {
        if (active) applyResponse(response);
      })
      .catch(() => {
        if (active) {
          send({
            type: 'set-error',
            error: 'Unable to load your cart. Please try again.',
          });
          send({ type: 'set-loading', isLoading: false });
        }
      });

    return () => {
      active = false;
    };
  }, [applyResponse, send]);

  const mutate = useCallback(
    async (
      optimisticItems: CartItem[],
      request: () => Promise<Response>,
      previousItems: CartItem[],
    ) => {
      send({ type: 'set-error', error: null });
      send({ type: 'set-items', items: optimisticItems });
      try {
        const response = await readCartResponse(await request());
        applyResponse(response);
      } catch (error) {
        send({ type: 'set-items', items: previousItems });
        send({
          type: 'set-error',
          error:
            error instanceof Error
              ? `${error.message} Please try again.`
              : 'Unable to update your cart. Please try again.',
        });
      }
    },
    [applyResponse, send],
  );

  const addToCart = useCallback(
    async (book: Book) => {
      let bookId: number;
      try {
        bookId = getBookId(String(book.id));
      } catch (error) {
        send({
          type: 'set-error',
          error: error instanceof Error ? error.message : 'Invalid book ID.',
        });
        return;
      }

      const normalizedBook = { ...book, id: String(book.id) };
      const previousItems = stateRef.current.items;
      const existing = previousItems.find(
        (item) => item.book.id === normalizedBook.id,
      );
      const optimisticItems = existing
        ? previousItems.map((item) =>
            item.book.id === normalizedBook.id
              ? { ...item, quantity: item.quantity + 1 }
              : item,
          )
        : [
            ...previousItems,
            {
              id: `pending-${normalizedBook.id}`,
              book: normalizedBook,
              quantity: 1,
            },
          ];
      await mutate(
        optimisticItems,
        () =>
          fetch(`${CART_API_URL}/items`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bookId, quantity: 1 }),
          }),
        previousItems,
      );
    },
    [mutate, send],
  );

  const removeFromCart = useCallback(
    async (bookId: string) => {
      const item = stateRef.current.items.find(
        (cartItem) => cartItem.book.id === bookId,
      );
      if (!item) return;

      const previousItems = stateRef.current.items;
      await mutate(
        previousItems.filter((cartItem) => cartItem.book.id !== bookId),
        () =>
          fetch(`${CART_API_URL}/items/${encodeURIComponent(item.id)}`, {
            method: 'DELETE',
            credentials: 'include',
          }),
        previousItems,
      );
    },
    [mutate],
  );

  const updateQuantity = useCallback(
    async (bookId: string, quantity: number) => {
      if (!Number.isInteger(quantity)) {
        send({ type: 'set-error', error: 'Cart quantity must be a whole number.' });
        return;
      }
      if (quantity <= 0) {
        await removeFromCart(bookId);
        return;
      }

      const item = stateRef.current.items.find(
        (cartItem) => cartItem.book.id === bookId,
      );
      if (!item || item.quantity === quantity) return;

      const previousItems = stateRef.current.items;
      await mutate(
        previousItems.map((cartItem) =>
          cartItem.book.id === bookId ? { ...cartItem, quantity } : cartItem,
        ),
        () =>
          fetch(`${CART_API_URL}/items/${encodeURIComponent(item.id)}`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity }),
          }),
        previousItems,
      );
    },
    [mutate, removeFromCart, send],
  );

  const checkout = useCallback(async (
    idempotencyKey: string,
  ): Promise<OrderResponse | null> => {
    send({ type: 'set-error', error: null });
    try {
      const response = await fetch(CHECKOUT_API_URL, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Idempotency-Key': idempotencyKey },
      });
      if (!response.ok) {
        let message = 'Checkout failed.';
        try {
          const body: unknown = await response.json();
          if (
            typeof body === 'object' &&
            body !== null &&
            'message' in body &&
            typeof body.message === 'string'
          ) {
            message = `Checkout failed. ${body.message}`;
          }
        } catch {
          // Gateway errors may not return JSON.
        }
        throw new Error(message);
      }

      const order = (await response.json()) as OrderResponse;
      send({ type: 'set-items', items: [] });
      send({ type: 'set-error', error: null });
      return order;
    } catch (error) {
      send({
        type: 'set-error',
        error:
          error instanceof Error
            ? `${error.message} Please try again.`
            : 'Checkout failed. Please try again.',
      });
      return null;
    }
  }, [send]);

  return (
    <CartContext.Provider
      value={{
        ...state,
        addToCart,
        updateQuantity,
        removeFromCart,
        checkout,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}
