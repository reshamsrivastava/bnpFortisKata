import { http, HttpResponse } from 'msw';
import type { LoginPayload, RegisterPayload } from '../types/auth';
import type { Book } from '../types/bookstore';

export const mockBooks: Book[] = [
  {
    id: '1',
    title: 'Clean Code',
    author: 'Robert C. Martin',
    price: 34.99,
    description: 'A handbook of agile software craftsmanship',
    stock: 10
  },
  {
    id: '2',
    title: 'Test Driven Development: By Example',
    author: 'Kent Beck',
    price: 29.99,
    description: 'Practical guide to TDD workflows',
    stock: 20
  },
];

export const handlers = [
  http.get('http://localhost:8080/api/books', () => {
    return HttpResponse.json(
      mockBooks, {status: 200});
  }),

  http.post('http://localhost:8080/api/users/login', async ({ request }) => {
    const { username, password } = (await request.json()) as LoginPayload;
    if (password === 'wrongpassword') {
      return HttpResponse.json(
        { message: 'Invalid username or password' },
        { status: 401 }
      );
    }
    return HttpResponse.json(
      { username, token: 'mock-jwt-token-12345' },
      { status: 200 }
    );
  }),

  http.post('http://localhost:8080/api/users/register', async ({ request }) => {
    const { username, email } = (await request.json()) as RegisterPayload;
    if (username === 'existinguser') {
      return HttpResponse.json(
        { message: 'Username already taken' },
        { status: 400 }
      );
    }
    return HttpResponse.json(
      { username, email, token: 'mock-jwt-token-67890' },
      { status: 201 }
    );
  }),
];