package com.chattalkie.repositories

import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.http.Method
import java.io.InputStream
import java.util.concurrent.TimeUnit

class StorageRepository(
    private val minioClient: MinioClient,
    private val bucketName: String = "chattalkie-media"
) {

    init {
        // Bucket yoksa oluştur
        try {
            val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                println("MinIO: Bucket '$bucketName' oluşturuldu")
            } else {
                println("MinIO: Bucket '$bucketName' mevcut")
            }
        } catch (e: Exception) {
            println("MinIO: Bucket kontrolü/oluşturma hatası: ${e.message}")
        }
    }

    suspend fun generateUploadUrl(objectName: String, contentType: String): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .`object`(objectName)
                .expiry(10, TimeUnit.MINUTES)
                .build()
        )
    }

    suspend fun generateDownloadUrl(objectName: String): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectName)
                .expiry(1, TimeUnit.HOURS)
                .build()
        )
    }
}

