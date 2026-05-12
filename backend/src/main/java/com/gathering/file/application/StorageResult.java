package com.gathering.file.application;

public record StorageResult(
	String publicUrl,
	String storagePath,
	String storedFilename
) {
}
