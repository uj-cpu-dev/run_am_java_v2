package com.api.sisi_yemi.util;

import com.api.sisi_yemi.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class ImageUploader {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-days:7}")
    private int presignedUrlExpiryDays;

    public ImageUploader(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadImageToS3(MultipartFile file, String userId) {
        try {
            String key = generateS3Key(file.getOriginalFilename(), userId);
            uploadToS3(file, key);
            return generatePreSignedUrl(key, Duration.ofDays(presignedUrlExpiryDays));
        } catch (IOException e) {
            log.error("Failed to upload image to S3: {}", e.getMessage());
            throw new ApiException("Failed to upload image to S3", HttpStatus.BAD_REQUEST, "IMAGE_UPLOAD_FAILED");
        }
    }

    public void deleteImageFromS3(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete image from S3 with key: {}", key, e);
            throw new ApiException("Failed to delete image from S3", HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_DELETE_FAILED");
        }
    }

    private String generateS3Key(String filename, String userId) {
        return String.format("ads/%s/%s_%s",
                userId,
                UUID.randomUUID(),
                sanitizeFilename(filename));
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private void uploadToS3(MultipartFile file, String key) throws IOException {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    public String generatePreSignedUrl(String objectKey, Duration expirationTime) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expirationTime)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest preSignedRequest = s3Presigner.presignGetObject(preSignRequest);
            return preSignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            throw new ApiException("Failed to generate image URL", HttpStatus.INTERNAL_SERVER_ERROR, "URL_GENERATION_FAILED");
        }
    }

    public String extractS3KeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (URISyntaxException e) {
            log.error("Invalid S3 URL format: {}", url);
            throw new ApiException("Invalid image URL", HttpStatus.BAD_REQUEST, "INVALID_IMAGE_URL");
        }
    }
}
