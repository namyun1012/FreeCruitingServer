package com.project.freecruting.service.storage;

import com.project.freecruting.config.AWS.S3Config;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
/*
 S3 File 저장소 사용시
 */

@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class S3FileService implements FileService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    // 현재 이미지만 지원
    private final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private final List<String> ALLOWED_IMAGE_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );


    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new NotFoundException("업로드 할 파일이 비었습니다.");
        }

        if (!isValidImageFile(file)) {
            throw new InvalidStateException("지원하지 않는 이미지 파일 형식 입니다.");
        }

        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newFileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(),file.getSize()));

            return newFileName;
        } catch (S3Exception e) {
            throw new IOException("S3 Error" + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        if(fileName == null || fileName.isEmpty()) {
            return;
        }

        // DB에 API 형식으로 저장된 경우 처리
        fileName = extractActualFileName(fileName);

        try {
            if (!fileExists(fileName)) {
                System.out.println("there is no file in S3: " + fileName );
                return;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.println("File Delete Success in S3 : " + fileName);
        }
        catch (S3Exception e) {
            throw new IOException("Deleteing file failed : " + e.getMessage());
        }
    }

    @Override
    public Path getFilePath(String fileName) throws IOException {
        String actualFileName = extractActualFileName(fileName);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(actualFileName)
                    .build();

            String fileExtension = getFileExtension(actualFileName);
            String tempFilePrefix = "s3-download-";
            String tempFileSuffix = fileExtension.isEmpty() ? ".tmp" : "." + fileExtension;

            Path tempFile = Files.createTempFile(tempFilePrefix, tempFileSuffix);

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            Files.copy(s3Object, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        }

        catch (S3Exception e) {
            throw new IOException("Failed in S3 Download: " + e.getMessage());
        }

    }

    public String getFileDirecturl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, fileName);
    }

    private boolean isValidImageFile(MultipartFile file) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String contentType = file.getContentType();

        boolean isExtensionAllowed = ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension.toLowerCase());
        boolean isMimeTypeAllowed = contentType != null && ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase());

        return isExtensionAllowed && isMimeTypeAllowed;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        // 확장자 없는 경우
        return "";
    }

    private boolean fileExists(String fileName) throws IOException {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            throw new IOException("S3 Error" + e.getMessage());
        }
    }

    // API 경로에서 실제 파일명 추출 (deleteFile, getFilePath에서 사용)
    private String extractActualFileName(String fileName) {
        if (fileName.startsWith("/api/v1/files/")) {
            return fileName.substring("/api/v1/files/".length());
        }
        // 이미 실제 파일명인 경우
        return fileName;
    }

}
