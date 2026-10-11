import { Grid, Box, Text, Callout } from '@radix-ui/themes';
import type { Book } from '../../types/bookstore';
import { useBooks } from '../../hooks/useBooks';
import { BookCard } from './BookCard';

interface BookListProps {
  onAddToCart: (book: Book) => void;
}

export function BookList({ onAddToCart }: BookListProps) {
  const { books, isLoading, error } = useBooks();

  if (isLoading) {
    return (
      <Box p="4" role="status">
        <Text style={{ color: '#94a3b8' }}>Loading books...</Text>
      </Box>
    );
  }

  if (error) {
    return (
      <Box p="4">
        <Callout.Root color="red" role="alert">
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  return (
    <Grid
      columns={{ initial: '1', sm: '2', md: '3' }}
      gap="4"
      width="100%"
      style={{ marginTop: '16px' }}
    >
      {books.map((book) => (
        <BookCard key={book.id} book={book} onAddToCart={onAddToCart} />
      ))}
    </Grid>
  );
}