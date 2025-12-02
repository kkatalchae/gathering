package com.gathering.user.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gathering.user.domain.model.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, String> {
	boolean existsByEmail(String email);

	boolean existsByPhoneNumber(String phoneNumber);

	Optional<UsersEntity> findByEmail(String email);
}
