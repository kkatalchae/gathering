package com.gathering.user.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.util.CryptoUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserJoinValidateService {

	private final UsersRepository usersRepository;

	@Value("${crypto.aes.key}")
	private String aesKey;
	public static final String ERROR_MESSAGE_EMAIL_DUPLICATE = "이미 사용중인 이메일입니다.";
	public static final String ERROR_MESSAGE_EMAIL_INVALID_FORMAT = "올바른 형식의 이메일이 아닙니다.";
	public static final String ERROR_MESSAGE_PHONE_NUMBER_DUPLICATE = "이미 사용중인 전화번호입니다.";
	public static final String ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT = "올바른 형식의 전화번호가 아닙니다.";
	public static final String ERROR_MESSAGE_PASSWORD_INVALID_FORMAT = "올바른 형식의 비밀번호가 아닙니다.";

	public void validateUser(UserJoinRequest request) {
		// email
		String email = request.getEmail();
		isValidEmailFormat(email);
		isEmailDuplicate(email);

		// phoneNumber
		String phoneNumber = request.getPhoneNumber();
		isPhoneNumberValidFormat(phoneNumber);
		isPhoneNumberDuplicate(phoneNumber);

		// password
		String encryptedPassword = request.getPassword();
		try {
			String decryptedPassword = CryptoUtil.decryptAES(encryptedPassword, aesKey);
			isValidPasswordFormat(decryptedPassword);
		} catch (Exception e) {
			throw new IllegalArgumentException(ERROR_MESSAGE_PASSWORD_INVALID_FORMAT);
		}
	}

	private void isEmailDuplicate(String email) {
		if (usersRepository.existsByEmail(email)) {
			throw new IllegalArgumentException(ERROR_MESSAGE_EMAIL_DUPLICATE);
		}
	}

	private void isValidEmailFormat(String email) {
		String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
		if (email == null || !email.matches(emailRegex)) {
			throw new IllegalArgumentException(ERROR_MESSAGE_EMAIL_INVALID_FORMAT);
		}
	}

	private void isValidPasswordFormat(String password) {
		// 비밀번호 형식 검증 로직 추가 (예: 최소 길이, 특수 문자 포함 등)
		// 예시: 최소 8자 이상, 숫자 및 특수 문자 포함
		String passwordRegex = "^(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,}$";
		if (password == null || !password.matches(passwordRegex)) {
			throw new IllegalArgumentException(ERROR_MESSAGE_PASSWORD_INVALID_FORMAT);
		}
	}

	private void isPhoneNumberDuplicate(String phoneNumber) {
		if (usersRepository.existsByPhoneNumber(phoneNumber)) {
			throw new IllegalArgumentException(ERROR_MESSAGE_PHONE_NUMBER_DUPLICATE);
		}
	}

	private void isPhoneNumberValidFormat(String phoneNumber) {
		String phoneNumberRegex = "^\\d{10,11}$"; // 10자리 또는 11자리 숫자
		if (phoneNumber == null || !phoneNumber.matches(phoneNumberRegex)) {
			throw new IllegalArgumentException(ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT);
		}
	}
}
