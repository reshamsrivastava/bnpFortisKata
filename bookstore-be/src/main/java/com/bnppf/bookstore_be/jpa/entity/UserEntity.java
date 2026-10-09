package com.bnppf.bookstore_be.jpa.entity;

import com.bnppf.bookstore_be.constants.UserConstraints;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = UserConstraints.MIN_USERNAME_LENGTH, max = UserConstraints.MAX_USERNAME_LENGTH)
    @Column(nullable = false, unique = true, length = UserConstraints.MAX_USERNAME_LENGTH)
    private String username;

    @NotBlank
    @Email
    @Size(max = UserConstraints.MAX_EMAIL_LENGTH)
    @Column(nullable = false, unique = true, length = UserConstraints.MAX_EMAIL_LENGTH)
    private String email;

    @NotBlank
    @Size(min = UserConstraints.MIN_PASSWORD_LENGTH, max = UserConstraints.MAX_PASSWORD_LENGTH)
    @Column(name = "password", nullable = false)
    private String password;

    public UserEntity(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
