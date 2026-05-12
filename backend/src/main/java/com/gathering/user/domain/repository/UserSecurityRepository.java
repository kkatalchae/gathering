package com.gathering.user.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gathering.user.domain.model.UserSecurityEntity;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurityEntity, String> {
	Optional<UserSecurityEntity> findByUserTsid(String userTsid);
}
