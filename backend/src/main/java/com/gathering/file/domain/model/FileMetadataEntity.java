package com.gathering.file.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "file_metadata",
	indexes = {
		@Index(name = "idx_file_metadata_uploader_tsid", columnList = "uploader_tsid"),
		@Index(name = "idx_file_metadata_file_type", columnList = "file_type")
	}
)
public class FileMetadataEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "stored_filename", nullable = false, length = 255)
	private String storedFilename;

	@Column(name = "storage_path", nullable = false, length = 500)
	private String storagePath;

	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "uploader_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String uploaderTsid;

	@Column(name = "file_type", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private FileType fileType;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant uploadedAt;

	@Builder
	public FileMetadataEntity(String originalFilename, String storedFilename, String storagePath,
		String mimeType, long fileSize, String uploaderTsid, FileType fileType) {
		this.originalFilename = originalFilename;
		this.storedFilename = storedFilename;
		this.storagePath = storagePath;
		this.mimeType = mimeType;
		this.fileSize = fileSize;
		this.uploaderTsid = uploaderTsid;
		this.fileType = fileType;
	}
}
