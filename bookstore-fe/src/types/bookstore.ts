export interface Book {
  id: string;
  title: string;
  author: string;
  price: number;
  stock: number;
  description?: string;
}

export interface CartItem {
  book: Book;
  quantity: number;
}