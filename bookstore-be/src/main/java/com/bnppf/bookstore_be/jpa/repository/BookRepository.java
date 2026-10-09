package com.bnppf.bookstore_be.jpa.repository;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    boolean existsByIsbn(String isbn);

    Optional<BookEntity> findByIsbn(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from BookEntity book where book.id in :ids order by book.id")
    List<BookEntity> findAllByIdInForUpdate(@Param("ids") List<Long> ids);
}
