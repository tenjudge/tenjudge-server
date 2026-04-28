package io.github.yush1x.tenjudge.server.infra;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 确保存储桶存在，如果不存在则自动创建。
     *
     * @throws Exception 当检查或创建桶失败时抛出异常
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 上传 MultipartFile 文件到 MinIO，会覆盖原有同名对象
     * 自动检查并生成桶
     *
     * @param file       上传的文件
     * @param objectName 存储到 MinIO 中的对象名称
     * @throws Exception 文件读取失败或上传失败时抛出异常
     */
    public void upload(MultipartFile file, String objectName) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            upload(inputStream, objectName, file.getSize(), file.getContentType());
        }
    }

    /**
     * 上传 InputStream 到 MinIO，会覆盖原有同名对象
     * 自动检查并生成桶
     *
     * @param inputStream 上传的输入流（由调用方负责创建）
     * @param objectName  存储到 MinIO 中的对象名称
     * @param size        输入流总字节数，未知时可传 -1
     * @param contentType 对象 MIME 类型，可为 null
     * @throws Exception 输入流读取失败或上传失败时抛出异常
     */
    public void upload(InputStream inputStream, String objectName, long size, String contentType) throws Exception {
        ensureBucketExists();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1L)
                        .contentType(contentType)
                        .build()
        );
    }

    /**
     * 上传本地文件到 MinIO，
     * 自动检查并生成桶
     * 会覆盖原有同名对象
     *
     * @param filePath   本地文件路径
     * @param objectName 存储到 MinIO 中的对象名称
     * @throws Exception 本地文件不存在、文件读取失败或上传失败时抛出异常
     */
    public void upload(Path filePath, String objectName) throws Exception {
        ensureBucketExists();

        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(filePath.toString())
                        .build()
        );
    }

    /**
     * 上传字符串内容到 MinIO，会覆盖原有同名对象
     * 自动检查并生成桶
     * 使用 UTF-8 编码并按 text/plain; charset=UTF-8 存储
     *
     * @param content    要上传的字符串内容
     * @param objectName 存储到 MinIO 中的对象名称
     * @throws Exception 内容编码失败或上传失败时抛出异常
     */
    public void upload(String content, String objectName) throws Exception {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
            upload(inputStream, objectName, bytes.length, "text/plain; charset=UTF-8");
        }
    }

    /**
     * 从 MinIO 获取对象。
     *
     * @param objectName MinIO 中的对象名称
     * @return 对象输入流，调用方使用完毕后需要自行关闭
     * @throws Exception 当对象不存在或下载失败时抛出异常
     */
    public InputStream get(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 从 MinIO 读取纯文本对象内容为字符串
     * 注意读取文件内容不宜超过1MB，避免占用过量内存
     * 文件内容须使用UTF-8编码
     * @param objectName MinIO 中的对象名称
     * @return 对象内容字符串
     * @throws Exception 当对象不存在读取失败时抛出异常
     */
    public String read(String objectName) throws Exception {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        )) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 从 MinIO 下载对象到本地。
     * 会自动生成目录结构，会覆盖原有文件
     *
     * @param objectName MinIO 中的对象名称
     * @param filePath 本地文件保存路径
     * @throws Exception 当对象不存在或下载失败时抛出异常
     */
    public void download(String objectName, Path filePath) throws Exception {
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(filePath.toString())
                        .build()
        );
    }

    /**
     * 删除 MinIO 中的对象（幂等）
     *
     * @param objectName MinIO 中的对象名称
     * @throws Exception 桶不存在或对象删除失败时抛出异常
     */
    public void delete(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除 MinIO 中以指定前缀开头的所有对象（幂等）
     * @param prefix 需要删除的对象前缀
     * @throws Exception 对象删除失败时抛出异常
     */
    public void deleteByPrefix(String prefix) throws Exception {
        String normalizedPrefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        List<DeleteObject> objectsToDelete = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(normalizedPrefix)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            objectsToDelete.add(new DeleteObject(item.objectName()));
        }

        if (objectsToDelete.isEmpty()) {
            return;
        }

        Iterable<Result<DeleteError>> removeResults = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(objectsToDelete)
                        .build()
        );

        // removeObjects 是惰性的，需遍历结果才能真正拿到删除错误
        for (Result<DeleteError> result : removeResults) {
            DeleteError error = result.get();
            throw new RuntimeException("Delete failed: object=" + error.objectName() + ", message=" + error.message());
        }
    }

    /**
     * 获取对象的预签名访问链接。
     *
     * @param objectName MinIO 中的对象名称
     * @return 有效期为 1 小时的预签名访问 URL
     * @throws Exception 当生成预签名链接失败时抛出异常
     */
    public String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(60, TimeUnit.MINUTES)
                        .build()
        );
    }


}
