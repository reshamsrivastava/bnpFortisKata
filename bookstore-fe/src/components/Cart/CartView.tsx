import {
  AlertDialog,
  Box,
  Button,
  Callout,
  Dialog,
  Flex,
  Table,
  Text,
} from '@radix-ui/themes';
import { useCart } from '../../hooks/useCart';
import type { CartItem } from '../../types/bookstore';

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
  } = useCart();

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
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

        {isLoading ? (
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

        <Flex justify="between" align="center" mt="5" pt="4">
          <Text weight="bold">Subtotal: {formatPrice(totalPrice)}</Text>
          <Flex gap="2">
            <Dialog.Close>
              <Button variant="soft" color="gray">
                Continue shopping
              </Button>
            </Dialog.Close>
            <Button disabled={isLoading || items.length === 0}>Checkout</Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
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
