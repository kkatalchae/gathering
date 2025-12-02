package com.gathering.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gathering.common.deserializer.AesEncryptedDeserializer;

/**
 * AES로 암호화된 문자열 필드에 붙이는 어노테이션
 * JSON 역직렬화 시 자동으로 복호화됩니다.
 *
 * 사용 예시:
 * <pre>
 * public class LoginRequest {
 *     private String email;
 *
 *     @AesEncrypted
 *     private String password;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = AesEncryptedDeserializer.class)
public @interface AesEncrypted {
}