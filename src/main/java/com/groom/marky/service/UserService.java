package com.groom.marky.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.marky.common.exception.IncorrectPasswordException;
import com.groom.marky.common.exception.UserAlreadyExistException;
import com.groom.marky.domain.request.LoginRequest;
import com.groom.marky.domain.LoginType;
import com.groom.marky.domain.User;
import com.groom.marky.domain.request.CreateUserRequest;
import com.groom.marky.domain.response.UserResponse;
import com.groom.marky.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
		this.userRepository = userRepository;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
	}

	@Transactional
	public UserResponse register(CreateUserRequest request) {

		// 이메일, LoginType 중복체크
		String userEmail = request.getUserEmail();
		request.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));

		boolean isAlreadyExist = userRepository.existsUsersByUserEmailAndLoginType(userEmail, LoginType.LOCAL);

		if (isAlreadyExist) {
			throw new UserAlreadyExistException(userEmail, LoginType.LOCAL);
		}

		// 엔티티는 DTO 를 몰라야 한다.
		User user = request.toUser();

		User savedUser = userRepository.save(user);

		// 엔티티는 DTO 를 몰라야 한다.
		return UserResponse.from(savedUser);

	}

	public UserResponse validate(LoginRequest request) {

		String userEmail = request.getUserEmail();
		String password = request.getPassword();

		User user = userRepository.findUserByUserEmail(userEmail).orElseThrow(
			() -> new EntityNotFoundException("해당하는 유저 정보가 존재하지 않습니다.")
		);

		boolean isSamePassword = user.isSamePassword(password, bCryptPasswordEncoder);

		if (!isSamePassword) {
			throw new IncorrectPasswordException();
		}

		return UserResponse.from(user);

	}
}
