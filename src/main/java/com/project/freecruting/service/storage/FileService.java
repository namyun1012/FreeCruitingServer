package com.project.freecruting.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileService {

    String uploadFile(MultipartFile file) throws IOException;
    void deleteFile(String fileName) throws IOException;
    Path getFilePath(String fileName) throws IOException;
}
