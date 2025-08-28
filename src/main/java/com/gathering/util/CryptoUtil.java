package com.gathering.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
	public static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";

	public static String decryptAES(String encrypted, String key) {
		try {
			byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
			SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);
			byte[] decrypted = cipher.doFinal(decoded);
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("비밀번호 복호화 실패", e);
		}
	}
}
