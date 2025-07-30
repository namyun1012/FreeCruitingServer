package com.project.freecruting.service.storage;

import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Primary
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileService implements FileService{

    @Value("${file.upload-dir}")
    private String uploadDir;
    
    // 현재 이미지만 지원
    private final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private final List<String> ALLOWED_IMAGE_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    public String uploadFile(MultipartFile file) throws IOException {
        if(file.isEmpty()) {
            throw new NotFoundException("업로드 할 파일이 비었습니다.");
        }

        if(!isValidImageFile(file)) {
            throw new InvalidStateException("지원하지 않는 이미지 파일 형식 입니다.");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath); // 없을 시 dir 생성

        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;
        Path targetLocation = uploadPath.resolve(newFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return newFileName;
    }
    
    // 현재 파일을 DB 에는 API 형식으로 저장 중이라 생김
    public void deleteFile(String fileName) throws IOException {
        if(fileName ==null || fileName.isEmpty()) {
            return;
        }
        
        // 로컬 파일이 아님
        if (!fileName.startsWith("/api/v1/files/")) {
            return;
        }

        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

        Path targetLocation = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);

        if (Files.exists(targetLocation)) {
            Files.delete(targetLocation);
            System.out.println("Success Deleting File" + fileName);
        }
        
        else {
            System.out.println("There is no file" + fileName);
        }
    }

    // 저장된 파일 불러오는 경로
    public Path getFilePath(String fileName) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
    }

    private boolean isValidImageFile(MultipartFile file) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String contentType = file.getContentType();

        boolean isExtensionAllowed = ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension);

        boolean isMimeTypeAllowed = contentType != null && ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase());
        return isExtensionAllowed && isMimeTypeAllowed;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        // 확장자 없는 경우
        return "";
    }
    
    

}
