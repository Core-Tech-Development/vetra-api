package dev.vetra.api.modules.imaging.service;

import dev.vetra.api.shared.config.MinioConfig;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;

/**
 * Wraps MinIO SDK for file operations on S3-compatible object storage.
 * All operations are blocking — callers must use worker threads.
 */
@ApplicationScoped
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    @Inject
    public MinioStorageService(MinioConfig config) {
        this.minioClient = MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(config.accessKey(), config.secretKey())
                .build();
        this.bucket = config.bucket();
    }

    public void uploadFile(String key, InputStream data, long size, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(data, size, -1)
                .contentType(contentType)
                .build());
    }

    public String getPresignedUrl(String key, int expirySeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket).object(key)
                .method(Method.GET)
                .expiry(expirySeconds)
                .build());
    }

    public void deleteFile(String key) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket).object(key).build());
    }
}
