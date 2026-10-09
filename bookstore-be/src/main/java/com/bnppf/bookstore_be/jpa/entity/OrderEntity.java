package com.bnppf.bookstore_be.jpa.entity;

import com.bnppf.bookstore_be.constants.OrderConstraints;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_orders", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_order_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @NotBlank
    @Column(
            name = "idempotency_key",
            nullable = false,
            length = OrderConstraints.MAX_IDEMPOTENCY_KEY_LENGTH)
    private String idempotencyKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItemEntity> items = new ArrayList<>();

    public OrderEntity(UserEntity user, String idempotencyKey) {
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public List<OrderItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        total = total.add(item.getSubtotal());
    }

    public void complete() {
        if (status != OrderStatus.PENDING || items.isEmpty()) {
            throw new IllegalStateException("Only a pending order with items can be completed");
        }
        status = OrderStatus.COMPLETED;
    }
}
