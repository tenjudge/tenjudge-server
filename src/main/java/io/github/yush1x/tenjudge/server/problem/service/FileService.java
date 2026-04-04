package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {

    // 将zip文件解压到指定目录
    public void unzip(MultipartFile file, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent()); // 确保父目录存在
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING); // 将流拷贝到目标路径
                }
                zis.closeEntry();
            }
        }
    }

    // 读取 config.yaml 文件并解析为 ProblemConfig 对象
    public ProblemConfig parseProblemConfig(Path path) throws Exception {
        Yaml yaml = new Yaml();
        ProblemConfig problemConfig;
        try (InputStream in = Files.newInputStream(path)) {
            problemConfig = yaml.loadAs(in, ProblemConfig.class);
        }
        return problemConfig;
    }

    // 读取纯文本文件内容
    public String readTextFile(Path path) throws IOException {
        return Files.readString(path);
    }

    // 移动文件
    public void moveFile(Path source, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // 覆盖目标文件
    }
}
