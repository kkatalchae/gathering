package com.gathering.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CryptoUtil {
	public static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";

	public static String encryptAES(String data, String key) throws Exception {
		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

		Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return java.util.Base64.getEncoder().encodeToString(encrypted);
	}

	// TODO Exception 전략 고민
	public static String decryptAES(String encrypted, String key) throws Exception {
		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
		byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);

		Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		byte[] decrypted = cipher.doFinal(decoded);
		return new String(decrypted, StandardCharsets.UTF_8);
	}
}
