package dev.vetra.api.shared.health;

import dev.vetra.api.shared.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.util.concurrent.TimeUnit;

/**
 * Readiness health check for MinIO object storage.
 * Verifies that the configured bucket exists and is reachable.
 * Exposed via /q/health/ready for Kubernetes probes and monitoring.
 */
@Readiness
@ApplicationScoped
public class MinioHealthCheck implements HealthCheck {

    private final MinioClient minioClient;
    private final String bucket;

    @Inject
    public MinioHealthCheck(MinioConfig config) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        this.minioClient = MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(config.accessKey(), config.secretKey())
                .httpClient(httpClient)
                .build();
        this.bucket = config.bucket();
    }

    @Override
    public HealthCheckResponse call() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (exists) {
                return HealthCheckResponse.named("MinIO")
                        .up()
                        .withData("bucket", bucket)
                        .build();
            }
            return HealthCheckResponse.named("MinIO")
                    .down()
                    .withData("bucket", bucket)
                    .withData("reason", "Bucket does not exist")
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("MinIO")
                    .down()
                    .withData("bucket", bucket)
                    .withData("reason", e.getMessage())
                    .build();
        }
    }
}
