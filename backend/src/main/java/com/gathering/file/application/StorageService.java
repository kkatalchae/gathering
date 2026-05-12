package com.gathering.file.application;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

	StorageResult store(MultipartFile file, String directory);

	void delete(String storagePath);
}
