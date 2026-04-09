package com.gathering.file.infra;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

	private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");
	private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
	private long maxProfileImageSize = 5 * 1024 * 1024L;    // 5MB
	private long maxGatheringImageSize = 10 * 1024 * 1024L; // 10MB
	private String localUploadPath = "uploads";
	private String urlPrefix = "/uploads";
}
