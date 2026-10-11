import { useState, useEffect, useCallback } from 'react';
import { apiFetch } from '../api/api';
import type { Book } from '../types/bookstore';

interface UseBooksReturn {
  books: Book[];
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useBooks(): UseBooksReturn {
  const [books, setBooks] = useState<Book[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBooks = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const res = await apiFetch('http://localhost:8080/api/books');
      const data: Book[] = await res.json();
      setBooks(data);
    } catch {
      setError('Failed to load books. Please try again later.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBooks();
  }, [fetchBooks]);

  return { books, isLoading, error, refetch: fetchBooks };
}