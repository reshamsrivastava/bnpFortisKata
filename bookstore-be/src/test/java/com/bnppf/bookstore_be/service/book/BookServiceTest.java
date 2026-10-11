package com.bnppf.bookstore_be.service.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private BookEntity book;

    @BeforeEach
    void setUp() {
        book = new BookEntity(
                "Effective Java",
                "Joshua Bloch",
                "9780134685991",
                "Programming",
                "Java best practices",
                new BigDecimal("42.00"),
                5);
        book.setId(12L);
    }

    @Test
    void shouldReturnAllBooksFromRepository() {
        List<BookEntity> books = List.of(book);
        when(bookRepository.findAll()).thenReturn(books);

        List<BookEntity> result = bookService.getAllBooks();

        assertSame(books, result);
        verify(bookRepository).findAll();
    }

    @Test
    void shouldReturnBookWhenIdExists() {
        when(bookRepository.findById(12L)).thenReturn(Optional.of(book));

        Optional<BookEntity> result = bookService.getBookById(12L);

        assertEquals(Optional.of(book), result);
        verify(bookRepository).findById(12L);
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), bookService.getBookById(999L));
        verify(bookRepository).findById(999L);
    }

    @Test
    void shouldRejectNullBookIdBeforeCallingRepository() {
        assertThrows(NullPointerException.class, () -> bookService.getBookById(null));
        verifyNoInteractions(bookRepository);
    }
}
