package com.bnppf.bookstore_be.service;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService{

    private final BookRepository bookRepository;

    @Override
    public List<BookEntity> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<BookEntity> getBookById(Long id) {
        return bookRepository.findById(id);
    }
}
