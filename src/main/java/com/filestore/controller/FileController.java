package com.filestore.controller;

import com.filestore.model.FileMetadata;
import com.filestore.service.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload a file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        FileMetadata fileMetadata = fileService.storeFile(file);
        
        String fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/files/download/")
            .path(fileMetadata.getFileName())
            .toUriString();
            
        Map<String, Object> response = new HashMap<>();
        response.put("fileName", fileMetadata.getOriginalFileName());
        response.put("fileId", fileMetadata.getId());
        response.put("fileDownloadUri", fileDownloadUrl);
        response.put("fileType", fileMetadata.getContentType());
        response.put("size", fileMetadata.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List all files
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllFiles() {
        List<FileMetadata> files = fileService.getAllFiles();
        
        List<Map<String, Object>> response = files.stream()
            .map(file -> {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", file.getId());
                fileInfo.put("name", file.getOriginalFileName());
                fileInfo.put("type", file.getFileType());
                fileInfo.put("size", file.getSize());
                fileInfo.put("uploadDate", file.getUploadDate());
                fileInfo.put("downloadUrl", ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(file.getFileName())
                    .toUriString());
                return fileInfo;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }
    
    /**
     * Download a file
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileService.loadFileAsResource(fileName);
        FileMetadata metadata = fileService.getFileMetadata(fileName);
        
        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Default content type if type could not be determined
            contentType = "application/octet-stream";
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
            .body(resource);
    }
    
    /**
     * Get file metadata
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String fileName) {
        FileMetadata metadata = fileService.getFileMetadata(fileName);
        return ResponseEntity.ok(metadata);
    }
}
