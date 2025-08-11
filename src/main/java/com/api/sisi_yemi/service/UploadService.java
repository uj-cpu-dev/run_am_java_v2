package com.api.sisi_yemi.service;

import com.api.sisi_yemi.controller.UploadController;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public UploadController.PresignedUrlResponse generatePresignedUrl(String id, String filename, String contentType, String folder) {
        String objectKey = generateS3Key(filename, id, folder);
        Duration expiration = Duration.ofMinutes(10);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(expiration)
                        .putObjectRequest(putObjectRequest)
                        .build());

        String publicUrl = generatePublicUrl(objectKey);

        return new UploadController.PresignedUrlResponse(
                filename,
                presignedRequest.url().toString(),
                publicUrl,
                objectKey
        );
    }

    private String generateS3Key(String filename, String adId, String folder) {
        String safeFolder = (folder != null && !folder.isEmpty()) ? folder : "ads";
        return String.format("%s/%s/%s_%s",
                safeFolder,
                adId,
                UUID.randomUUID(),
                sanitizeFilename(filename));
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String generatePublicUrl(String objectKey) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, objectKey);
    }
}
