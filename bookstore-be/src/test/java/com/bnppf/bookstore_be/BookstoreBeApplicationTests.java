package com.bnppf.bookstore_be;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnppf.bookstore_be.jpa.repository.CartRepository;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import com.bnppf.bookstore_be.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class BookstoreBeApplicationTests {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void initializesDemoDataOnStartup() {
		assertThat(bookRepository.existsByIsbn("9780135957059")).isTrue();
		assertThat(bookRepository.existsByIsbn("9780132350884")).isTrue();
		assertThat(bookRepository.existsByIsbn("9780547928227")).isTrue();
		assertThat(bookRepository.existsByIsbn("9780441478125")).isTrue();

		var demoUser = userRepository.findByUsername("demo").orElseThrow();
		assertThat(demoUser.getEmail()).isEqualTo("demo@bookstore.com");
		assertThat(passwordEncoder.matches("DemoPassword123", demoUser.getPassword())).isTrue();

		var demoCart = cartRepository.findByUser_Username("demo").orElseThrow();
		assertThat(demoCart.getItems()).hasSize(2);
	}

}
