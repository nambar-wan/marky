package com.groom.marky.service;

import static com.groom.marky.domain.LoginType.*;
import static com.groom.marky.domain.Role.*;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.marky.common.exception.IncorrectPasswordException;
import com.groom.marky.common.exception.UserAlreadyExistException;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.request.LoginRequest;
import com.groom.marky.domain.LoginType;
import com.groom.marky.domain.User;
import com.groom.marky.domain.request.CreateUserRequest;
import com.groom.marky.domain.response.GoogleUserInfo;
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

		boolean isAlreadyExist = userRepository.existsUsersByUserEmailAndLoginType(userEmail, LOCAL);

		if (isAlreadyExist) {
			throw new UserAlreadyExistException(userEmail, LOCAL);
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

	@Transactional
	public UserResponse findOrCreate(GoogleUserInfo userInfo) {
		// 1. 해당되는 구글 아이디가 있는지 확인. 있으면 해당 유저 반환
		String googleId = userInfo.getGoogleId();
		String userEmail = userInfo.getUserEmail();
		String name = userInfo.getName();

		Optional<User> userByGoogleId = userRepository.findUserByGoogleId(googleId);

		// 유저가 이미 있는 경우
		if (userByGoogleId.isPresent()) {
			User existedUser = userByGoogleId.get();
			return UserResponse.from(existedUser);
		}

		// 없다면 생성 후 반환. 패스워드는 비어있음. 로그인 타입은 구글
		User user = User.builder()
				.userEmail(userEmail)
				.googleId(googleId)
				.loginType(GOOGLE)
				.role(ROLE_USER)
				.name(name)
			.build();

		User savedUser = userRepository.save(user);

		return UserResponse.from(savedUser);

	}
}
