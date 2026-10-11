import { Card, Box, Heading, Text, Button, Flex } from '@radix-ui/themes';
import { type Book } from '../../types/bookstore';

interface BookCardProps {
  book: Book;
  onAddToCart: (book: Book) => void;
}

export function BookCard({ book, onAddToCart }: BookCardProps) {
  return (
    <Card
      size="2"
      variant="surface"
      style={{
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '12px',
        padding: '18px',
        backgroundColor: '#1e293b',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
      }}
    >
      <Box mb="4">
        <Heading as="h3" size="4" mb="1" style={{ color: '#f8fafc' }}>
          {book.title}
        </Heading>
        <Text as="p" size="2" style={{ color: '#94a3b8', marginBottom: '8px' }}>
          by {book.author}
        </Text>        
      </Box>

      <Flex justify="between" align="center" pt="3" style={{ borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
        <Text size="4" weight="bold" style={{ color: '#38bdf8' }}>
          ${book.price.toFixed(2)}
        </Text>
        <Button
          size="2"
          variant="solid"
          color="indigo"
          style={{ cursor: 'pointer', paddingInline: '14px' }}
          onClick={() => onAddToCart(book)}
        >
          Add to Cart
        </Button>
      </Flex>
    </Card>
  );
}