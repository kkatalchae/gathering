package com.gathering.schedule.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 장소 Value Object
 * 만날 곳이 없는 일정은 성립하지 않으므로 장소명은 필수이며, 상세 주소는 선택 값이다
 */
@Getter
public class ScheduleLocation {

	public static final int MAX_NAME_LENGTH = 100;
	public static final int MAX_ADDRESS_LENGTH = 255;

	private final String name;
	private final String address;

	/**
	 * 일정 장소 생성
	 *
	 * @param name 장소명 (필수)
	 * @param address 장소 주소 (null 허용)
	 * @throws BusinessException 장소명이 없거나, 장소명이 100자를, 주소가 255자를 초과하는 경우
	 */
	public ScheduleLocation(String name, String address) {
		if (name == null || name.isBlank()) {
			throw new BusinessException(ErrorCode.SCHEDULE_LOCATION_NAME_REQUIRED);
		}
		if (name.length() > MAX_NAME_LENGTH) {
			throw new BusinessException(ErrorCode.SCHEDULE_LOCATION_NAME_TOO_LONG);
		}
		if (address != null && address.length() > MAX_ADDRESS_LENGTH) {
			throw new BusinessException(ErrorCode.SCHEDULE_LOCATION_ADDRESS_TOO_LONG);
		}
		this.name = name;
		this.address = address;
	}
}
