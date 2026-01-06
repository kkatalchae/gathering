package com.gathering.gathering.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GatheringCategory {
	SPORTS("운동/스포츠"),
	CULTURE("문화/예술"),
	STUDY("스터디/교육"),
	HOBBY("취미/여가"),
	FOOD("맛집/요리"),
	TRAVEL("여행"),
	NETWORKING("네트워킹/친목"),
	VOLUNTEER("봉사활동"),
	GAME("게임/e스포츠"),
	PET("반려동물"),
	PHOTOGRAPHY("사진/영상"),
	MUSIC("음악/공연"),
	BOOK("독서/토론"),
	TECH("IT/기술"),
	INVESTMENT("재테크/투자"),
	ETC("기타");

	private final String description;

}
