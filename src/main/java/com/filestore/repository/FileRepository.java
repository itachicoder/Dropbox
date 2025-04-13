package com.filestore.repository;

import com.filestore.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    
    Optional<FileMetadata> findByFileName(String fileName);
    
    boolean existsByFileName(String fileName);
}
