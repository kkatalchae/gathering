package com.gathering.common.deserializer;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gathering.util.CryptoUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * AES로 암호화된 문자열을 복호화하는 Jackson Deserializer
 */
@Slf4j
@Component
public class AesEncryptedDeserializer extends JsonDeserializer<String> {

	private static String aesKey;

	@Value("${crypto.aes.key}")
	public void setAesKey(String key) {
		AesEncryptedDeserializer.aesKey = key;
	}

	@Override
	public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		String encryptedValue = parser.getText();

		if (encryptedValue == null || encryptedValue.isEmpty()) {
			return encryptedValue;
		}

		try {
			String decrypted = CryptoUtil.decryptAES(encryptedValue, aesKey);
			log.debug("AES 암호화된 데이터 복호화 성공");
			return decrypted;
		} catch (Exception e) {
			log.error("AES 복호화 실패: {}", e.getMessage());
			throw new IOException("AES 암호화된 데이터 복호화에 실패했습니다", e);
		}
	}
}