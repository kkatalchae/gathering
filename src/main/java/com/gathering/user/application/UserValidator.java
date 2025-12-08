package com.gathering.user.application;

import org.springframework.stereotype.Service;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 검증을 담당하는 Validator
 * 회원가입, 프로필 수정, 비밀번호 변경 등에서 재사용
 */
@Service
@RequiredArgsConstructor
public class UserValidator {

	private final UsersRepository usersRepository;

	/**
	 * 회원가입 시 사용자 정보 검증
	 *
	 * @param request 회원가입 요청 DTO
	 */
	public void validateForJoin(UserJoinRequest request) {
		validateEmailFormat(request.getEmail());
		validateEmailUnique(request.getEmail());
		validatePhoneNumberFormat(request.getPhoneNumber());
		validatePhoneNumberUnique(request.getPhoneNumber());
		validatePasswordFormat(request.getPassword());
	}

	/**
	 * 프로필 수정 시 전화번호 검증
	 * 현재 전화번호와 다를 때만 중복 검증
	 *
	 * @param currentPhoneNumber 현재 사용자의 전화번호
	 * @param newPhoneNumber 새로운 전화번호
	 */
	public void validatePhoneNumberForUpdate(String currentPhoneNumber, String newPhoneNumber) {
		if (newPhoneNumber == null) {
			return; // 변경하지 않음
		}

		// 형식 검증
		validatePhoneNumberFormat(newPhoneNumber);

		// 전화번호가 변경된 경우에만 중복 검증
		if (!newPhoneNumber.equals(currentPhoneNumber)) {
			validatePhoneNumberUnique(newPhoneNumber);
		}
	}

	/**
	 * 이름 검증
	 *
	 * @param name 검증할 이름
	 */
	public void validateName(String name) {
		if (name != null && name.isBlank()) {
			throw new BusinessException(ErrorCode.NAME_BLANK);
		}
	}

	/**
	 * 이메일 중복 검증
	 *
	 * @param email 검증할 이메일
	 */
	public void validateEmailUnique(String email) {
		if (usersRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
		}
	}

	/**
	 * 이메일 형식 검증
	 *
	 * @param email 검증할 이메일
	 */
	public void validateEmailFormat(String email) {
		String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
		if (email == null || !email.matches(emailRegex)) {
			throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
		}
	}

	/**
	 * 비밀번호 형식 검증
	 * 최소 8자 이상, 숫자 및 특수문자 포함
	 *
	 * @param password 검증할 비밀번호
	 */
	public void validatePasswordFormat(String password) {
		String passwordRegex = "^(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,}$";
		if (password == null || !password.matches(passwordRegex)) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
		}
	}

	/**
	 * 전화번호 중복 검증
	 *
	 * @param phoneNumber 검증할 전화번호
	 */
	public void validatePhoneNumberUnique(String phoneNumber) {
		if (usersRepository.existsByPhoneNumber(phoneNumber)) {
			throw new BusinessException(ErrorCode.PHONE_NUMBER_DUPLICATE);
		}
	}

	/**
	 * 전화번호 형식 검증
	 * 10-11자리 숫자만 허용
	 *
	 * @param phoneNumber 검증할 전화번호
	 */
	public void validatePhoneNumberFormat(String phoneNumber) {
		String phoneNumberRegex = "^\\d{10,11}$";
		if (phoneNumber == null || !phoneNumber.matches(phoneNumberRegex)) {
			throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER_FORMAT);
		}
	}
}