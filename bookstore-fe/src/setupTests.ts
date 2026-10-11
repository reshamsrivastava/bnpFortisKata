import '@testing-library/jest-dom/vitest';
import { beforeAll, afterEach, afterAll } from 'vitest';
import {server} from './mocks/server';

globalThis.ResizeObserver = class ResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    void callback;
  }

  observe() {}
  unobserve() {}
  disconnect() {}
};

// Establish API mocking before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Reset any runtime request handlers added during tests
afterEach(() => server.resetHandlers());

// Clean up after the tests are finished
afterAll(() => server.close());