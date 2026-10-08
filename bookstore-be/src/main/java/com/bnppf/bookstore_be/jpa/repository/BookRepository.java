package com.bnppf.bookstore_be.jpa.repository;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, Long> {
}
