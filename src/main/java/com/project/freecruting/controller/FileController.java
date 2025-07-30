package com.project.freecruting.controller;

import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.service.storage.FileService;
import com.project.freecruting.service.storage.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;


    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileService.uploadFile(file);
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/upload/files")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok("이미지 파일이 성공적으로 업로드되었습니다. 접근 URL: " + fileDownloadUri);
        }

        catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 중 오류 발생" + e.getMessage());
        }
    }
    
    // Image 보여줄 떄 호출 될 것
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadImage(@PathVariable String fileName) {
        try {
            Path filePath = fileService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());


            if(resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);

                if (contentType == null) {
                    contentType = "application/octet-stream"; // 알 수 없는 경우 기본값
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else{
                throw new NotFoundException("파일이 없거나 읽을 수 없습니다.");
            }
        }
        catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }

        catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
