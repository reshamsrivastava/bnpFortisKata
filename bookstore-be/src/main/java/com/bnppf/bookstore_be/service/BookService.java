package com.bnppf.bookstore_be.service;


import com.bnppf.bookstore_be.jpa.entity.BookEntity;

import java.util.List;
import java.util.Optional;

public interface BookService {
    List<BookEntity> getAllBooks() ;

    Optional<BookEntity> getBookById(Long id) ;
}
