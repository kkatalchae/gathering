package com.gathering.user.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gathering.user.domain.model.UserSecurityEntity;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurityEntity, Long> {

}
