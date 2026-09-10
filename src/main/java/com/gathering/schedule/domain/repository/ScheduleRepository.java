package com.gathering.schedule.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.schedule.domain.model.ScheduleEntity;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, String> {
}
