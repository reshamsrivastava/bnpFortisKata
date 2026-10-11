import { useRef, useState } from 'react';
import {
  AlertDialog,
  Box,
  Button,
  Callout,
  Dialog,
  Flex,
  Heading,
  Table,
  Text,
} from '@radix-ui/themes';
import { useCart } from '../../hooks/useCart';
import type { CartItem } from '../../types/bookstore';
import type { OrderResponse } from '../../types/order';

interface CartViewProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

function formatPrice(price: number): string {
  return currency.format(price);
}

export function CartView({ open, onOpenChange }: CartViewProps) {
  const {
    items,
    totalPrice,
    isLoading,
    error,
    updateQuantity,
    removeFromCart,
    checkout,
  } = useCart();
  const [checkoutItems, setCheckoutItems] = useState<CartItem[]>([]);
  const [checkoutTotal, setCheckoutTotal] = useState(0);
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [showOrderSummary, setShowOrderSummary] = useState(false);
  const idempotencyKey = useRef<string | null>(null);

  const handleCheckout = async () => {
    if (!showOrderSummary) {
      setCheckoutItems(items);
      setCheckoutTotal(totalPrice);
      setOrder(null);
      setShowOrderSummary(true);
    }
    idempotencyKey.current ??= crypto.randomUUID();
    const result = await checkout(idempotencyKey.current);
    if (result) setOrder(result);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      setShowOrderSummary(false);
      setOrder(null);
      idempotencyKey.current = null;
    }
    onOpenChange(nextOpen);
  };

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Content
        aria-label="Shopping cart"
        maxWidth="700px"
        style={{ maxHeight: '85vh', overflowY: 'auto' }}
      >
        <Flex justify="between" align="center" mb="4">
          <Dialog.Title>Shopping cart</Dialog.Title>
          <Dialog.Close>
            <Button variant="soft" color="gray" aria-label="Close cart">
              Close
            </Button>
          </Dialog.Close>
        </Flex>
        <Dialog.Description size="2" mb="4" color="gray">
          Review and update the books in your cart.
        </Dialog.Description>

        {error && (
          <Callout.Root color="red" role="alert" mb="4">
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}

        {showOrderSummary ? (
          order ? (
            <OrderConfirmation order={order} onDone={() => handleOpenChange(false)} />
          ) : (
            <OrderSummary
              items={checkoutItems}
              total={checkoutTotal}
              error={error}
              onRetry={handleCheckout}
            />
          )
        ) : isLoading ? (
          <Box py="6" role="status" aria-label="Loading cart">
            <Text>Loading your cart...</Text>
          </Box>
        ) : items.length === 0 ? (
          <Box py="6" role="status">
            <Text>Your cart is empty.</Text>
          </Box>
        ) : (
          <Table.Root variant="surface" aria-label="Shopping cart">
            <caption>Items in your cart</caption>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Book</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Unit price</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Quantity</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Subtotal</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {items.map((item) => (
                <CartRow
                  key={item.id}
                  item={item}
                  onUpdateQuantity={(quantity) =>
                    void updateQuantity(item.book.id, quantity)
                  }
                  onRemove={() => void removeFromCart(item.book.id)}
                />
              ))}
            </Table.Body>
          </Table.Root>
        )}

        {!showOrderSummary && (
          <Flex justify="between" align="center" mt="5" pt="4">
            <Text weight="bold">Subtotal: {formatPrice(totalPrice)}</Text>
            <Flex gap="2">
              <Dialog.Close>
                <Button variant="soft" color="gray">
                  Continue shopping
                </Button>
              </Dialog.Close>
              <Button
                disabled={isLoading || items.length === 0}
                onClick={() => void handleCheckout()}
              >
                Checkout
              </Button>
            </Flex>
          </Flex>
        )}
      </Dialog.Content>
    </Dialog.Root>
  );
}

function OrderSummary({
  items,
  total,
  error,
  onRetry,
}: {
  items: CartItem[];
  total: number;
  error: string | null;
  onRetry: () => Promise<void>;
}) {
  return (
    <Box>
      <Heading size="4" mb="3">
        Order summary
      </Heading>
      <Table.Root variant="surface" aria-label="Order summary">
        <caption>Items in your order</caption>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Book</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Unit price</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Quantity</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Subtotal</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {items.map((item) => (
            <Table.Row key={item.id}>
              <Table.RowHeaderCell>{item.book.title}</Table.RowHeaderCell>
              <Table.Cell>{formatPrice(item.book.price)}</Table.Cell>
              <Table.Cell>{item.quantity}</Table.Cell>
              <Table.Cell>{formatPrice(item.book.price * item.quantity)}</Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
      <Text as="p" weight="bold" mt="4">
        Total: {formatPrice(total)}
      </Text>
      {!error && (
        <Text as="p" size="2" color="gray" mt="2" role="status">
          Placing your order...
        </Text>
      )}
      {error && (
        <Button mt="3" onClick={() => void onRetry()}>
          Retry checkout
        </Button>
      )}
    </Box>
  );
}

function OrderConfirmation({
  order,
  onDone,
}: {
  order: OrderResponse;
  onDone: () => void;
}) {
  return (
    <Box role="status">
      <Heading size="4" mb="3">
        Order confirmed
      </Heading>
      <Text as="p">Order #{order.id}</Text>
      <Text as="p" mt="2">
        Status: {order.status}
      </Text>
      <Button mt="4" onClick={onDone}>
        Continue shopping
      </Button>
    </Box>
  );
}

function CartRow({
  item,
  onUpdateQuantity,
  onRemove,
}: {
  item: CartItem;
  onUpdateQuantity: (quantity: number) => void;
  onRemove: () => void;
}) {
  return (
    <Table.Row>
      <Table.RowHeaderCell>{item.book.title}</Table.RowHeaderCell>
      <Table.Cell>{formatPrice(item.book.price)}</Table.Cell>
      <Table.Cell>
        <Flex align="center" gap="2">
          <Button
            size="1"
            variant="soft"
            aria-label={`Decrease ${item.book.title} quantity`}
            onClick={() => onUpdateQuantity(item.quantity - 1)}
          >
            −
          </Button>
          <Text aria-label={`${item.book.title} quantity`}>
            {item.quantity}
          </Text>
          <Button
            size="1"
            variant="soft"
            aria-label={`Increase ${item.book.title} quantity`}
            onClick={() => onUpdateQuantity(item.quantity + 1)}
          >
            +
          </Button>
        </Flex>
      </Table.Cell>
      <Table.Cell>{formatPrice(item.book.price * item.quantity)}</Table.Cell>
      <Table.Cell>
        <AlertDialog.Root>
          <AlertDialog.Trigger>
            <Button
              size="1"
              variant="soft"
              color="red"
              aria-label={`Remove ${item.book.title}`}
            >
              Remove
            </Button>
          </AlertDialog.Trigger>
          <AlertDialog.Content maxWidth="450px">
            <AlertDialog.Title>Remove book from cart?</AlertDialog.Title>
            <AlertDialog.Description size="2">
              Remove {item.book.title} from your cart?
            </AlertDialog.Description>
            <Flex gap="3" mt="4" justify="end">
              <AlertDialog.Cancel>
                <Button variant="soft" color="gray">
                  Cancel
                </Button>
              </AlertDialog.Cancel>
              <AlertDialog.Action>
                <Button variant="solid" color="red" onClick={onRemove}>
                  Remove
                </Button>
              </AlertDialog.Action>
            </Flex>
          </AlertDialog.Content>
        </AlertDialog.Root>
      </Table.Cell>
    </Table.Row>
  );
}
