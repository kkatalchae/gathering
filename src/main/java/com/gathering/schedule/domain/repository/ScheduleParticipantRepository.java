package com.gathering.schedule.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.schedule.domain.model.ScheduleParticipantEntity;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipantEntity, String> {
}
