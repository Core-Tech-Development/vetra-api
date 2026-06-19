package dev.vetra.api.modules.imaging.service;

import dev.vetra.api.shared.config.MinioConfig;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.minioClient = MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(config.accessKey(), config.secretKey())
                .httpClient(httpClient)
                .build();
        this.bucket = config.bucket();
    }

    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 30000, successThreshold = 3)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500)
    @Timeout(60000)
    public void uploadFile(String key, InputStream data, long size, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(data, size, -1)
                .contentType(contentType)
                .build());
    }

    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 30000, successThreshold = 3)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500)
    @Timeout(60000)
    public String getPresignedUrl(String key, int expirySeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket).object(key)
                .method(Method.GET)
                .expiry(expirySeconds)
                .build());
    }

    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 30000, successThreshold = 3)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500)
    @Timeout(60000)
    public void deleteFile(String key) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket).object(key).build());
    }
}
