package com.bnppf.bookstore_be.controller;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.records.book.BookResponse;
import com.bnppf.bookstore_be.service.book.BookService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.of(bookService.getBookById(id).map(this::toResponse));
    }

    private BookResponse toResponse(BookEntity book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getDescription(),
                book.getPrice(),
                book.getStock());
    }
}
