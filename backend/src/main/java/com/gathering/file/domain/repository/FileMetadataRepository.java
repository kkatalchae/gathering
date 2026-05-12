package com.gathering.file.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.file.domain.model.FileMetadataEntity;
import com.gathering.file.domain.model.FileType;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, String> {

	List<FileMetadataEntity> findByUploaderTsidAndFileType(String uploaderTsid, FileType fileType);
}
