package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Transactional
	public void register(String username, String password) {

		if (userRepository.findByUserName(username).isPresent()) {
			throw new RuntimeException("USER_ALREADY_EXISTS");
		}
		User user = new User();
		user.setUserName(username);

		// パスワードをハッシュ化してセット
		user.setPassword(passwordEncoder.encode(password));
		user.setRole("ROLE_USER");

		userRepository.save(user);
	}
}