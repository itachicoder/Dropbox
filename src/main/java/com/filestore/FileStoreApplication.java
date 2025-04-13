package com.filestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.filestore.config.FileStorageConfig;

@SpringBootApplication
@EnableConfigurationProperties({
    FileStorageConfig.class
})
public class FileStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileStoreApplication.class, args);
    }
}
