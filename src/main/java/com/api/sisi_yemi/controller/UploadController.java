package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.service.UploadService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    private final AuthenticationHelper authHelper;

    @PostMapping("/presigned-urls")
    public ResponseEntity<?> generateMultiplePresignedUrls(
            @RequestBody MultiplePresignedUrlsRequest request) {

        String userId = authHelper.getAuthenticatedUserId();
        if(userId.isEmpty()){
            throw new ApiException("NOT_AUTHOURISED", HttpStatus.UNAUTHORIZED, "UNABLE_TO_AUTHENTICATE_USER");
        }

        try {
            List<PresignedUrlResponse> responses = request.files().stream()
                    .map(file -> uploadService.generatePresignedUrl(
                            request.adId(),
                            file.filename(),
                            file.contentType(),
                            request.folder()))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating presigned URLs");
        }
    }

    public record PresignedUrlRequest(String filename, String contentType) {}

    public record MultiplePresignedUrlsRequest(String adId, List<PresignedUrlRequest> files, String folder) {}

    public record PresignedUrlResponse(String filename, String uploadUrl, String publicUrl, String objectKey) {}
}
