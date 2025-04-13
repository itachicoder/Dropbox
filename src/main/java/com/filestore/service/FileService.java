package com.filestore.service;

import com.filestore.config.FileStorageConfig;
import com.filestore.exception.FileNotFoundException;
import com.filestore.exception.FileStorageException;
import com.filestore.model.FileMetadata;
import com.filestore.repository.FileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private final Path fileStorageLocation;
    private final FileRepository fileRepository;

    // List of allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", 
        "jpg", "jpeg", "png", "gif", "svg", "json", "xml", "html", "css", "js"
    ));

    @Autowired
    public FileService(FileStorageConfig fileStorageConfig, FileRepository fileRepository) {
        this.fileRepository = fileRepository;
        this.fileStorageLocation = fileStorageConfig.getUploadPath();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }

    /**
     * Stores a file on the file system and saves metadata in the database
     */
    public FileMetadata storeFile(MultipartFile file) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        if (originalFileName.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
        }
        
        // Check if file type is allowed
        String fileExtension = getFileExtension(originalFileName);
        if (!isValidFileType(fileExtension)) {
            throw new FileStorageException("File type not allowed: " + fileExtension);
        }
        
        try {
            // Generate unique file name to prevent overwriting
            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            
            // Copy file to the target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Save file metadata to database
            FileMetadata fileMetadata = FileMetadata.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileType(fileExtension)
                .contentType(file.getContentType())
                .size(file.getSize())
                .filePath(targetLocation.toString())
                .uploadDate(LocalDateTime.now())
                .build();
                
            return fileRepository.save(fileMetadata);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName, ex);
        }
    }

    /**
     * Retrieves all file metadata from the database
     */
    public List<FileMetadata> getAllFiles() {
        return fileRepository.findAll();
    }

    /**
     * Loads a file as a Resource by its file name
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            FileMetadata fileMetadata = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileName));
                
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("File not found: " + fileName, ex);
        }
    }
    
    /**
     * Get file metadata by filename
     */
    public FileMetadata getFileMetadata(String fileName) {
        return fileRepository.findByFileName(fileName)
            .orElseThrow(() -> new FileNotFoundException("File not found: " + fileName));
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Check if file type is allowed
     */
    private boolean isValidFileType(String fileExtension) {
        return ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }
}
