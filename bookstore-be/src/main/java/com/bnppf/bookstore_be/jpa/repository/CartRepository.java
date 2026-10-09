package com.bnppf.bookstore_be.jpa.repository;

import com.bnppf.bookstore_be.jpa.entity.CartEntity;
import com.bnppf.bookstore_be.records.cart.CartItemResponse;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUser_Username(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from CartEntity cart where cart.user.username = :username")
    Optional<CartEntity> findByUser_UsernameForUpdate(@Param("username") String username);

    @Query("""
            select new com.bnppf.bookstore_be.records.cart.CartItemResponse(
                item.id, book.id, book.title, item.quantity, item.unitPrice,
                item.unitPrice * item.quantity
            )
            from CartItemEntity item
            join item.book book
            where item.cart.id = :cartId
            order by item.id
            """)
    List<CartItemResponse> findItemResponsesByCartId(@Param("cartId") Long cartId);
}
