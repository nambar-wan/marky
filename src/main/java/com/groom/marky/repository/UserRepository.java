package com.groom.marky.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.marky.domain.LoginType;
import com.groom.marky.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findUserByUserEmail(String userEmail);

	Optional<User> findUserByGoogleId(String googleId);

	boolean existsUsersByUserEmailAndLoginType(String userEmail, LoginType loginType);
}
