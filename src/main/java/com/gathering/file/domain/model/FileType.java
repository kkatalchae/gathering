package com.gathering.file.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType {
	PROFILE_IMAGE(5 * 1024 * 1024L),
	GATHERING_IMAGE(10 * 1024 * 1024L);

	private final long maxSize;
}
