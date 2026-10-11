import { useState } from 'react';
import { Badge, Button } from '@radix-ui/themes';
import { useCart } from '../../hooks/useCart';
import { CartView } from './CartView';

export function CartLauncher() {
  const [open, setOpen] = useState(false);
  const { totalItems } = useCart();

  return (
    <>
      <Button
        variant="soft"
        color="indigo"
        aria-label={`Open shopping cart, ${totalItems} items`}
        onClick={() => setOpen(true)}
      >
        <Badge color="indigo" variant="soft">
          Cart ({totalItems})
        </Badge>
      </Button>
      <CartView open={open} onOpenChange={setOpen} />
    </>
  );
}
