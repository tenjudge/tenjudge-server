package io.github.yush1x.tenjudge.server.problem.storage;

import io.github.yush1x.tenjudge.server.problem.dto.ProblemConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {

    /**
     * 检查是否存在且是文件
     * @param path 需要检查的文件路径
     * @return 是否存在且是文件
     */
    public boolean isRegularFile(Path path) {
        return Files.isRegularFile(path);
    }


    /**
     * 将zip文件解压到指定目录
     *
     * @param file 待解压的文件
     * @param destDir 需要解压到的目录路径（根据业务需要，这里很可能是uuid临时目录）
     * @throws IOException 解压到指定路径失败
     */
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

    /**
     * 读取 yaml 文件并解析为 ProblemConfig 对象
     *
     * @param path yaml 文件路径
     * @return 解析得到的 ProblemConfig 对象
     * @throws IOException 文件读取失败
     */
    public ProblemConfig parseProblemConfig(Path path) throws IOException {
        Yaml yaml = new Yaml();
        ProblemConfig problemConfig;
        try (InputStream in = Files.newInputStream(path)) {
            problemConfig = yaml.loadAs(in, ProblemConfig.class);
        }
        return problemConfig;
    }

    /**
     * 读取纯文本文件内容
     *
     * @param path 需要读取的纯文本文件路径
     * @return 纯文本文件的字符串
     * @throws IOException 读取文件异常
     */
    public String readTextFile(Path path) throws IOException {
        return Files.readString(path);
    }


    /**
     * 移动文件
     * @param source 源文件路径
     * @param target 目标文件路径
     * @throws IOException 文件操作异常
     */
    public void moveFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // 覆盖目标文件
    }

    /**
     * 删除目录（幂等）
     * @param dir 需要被删除的目录
     */
    public void deleteDirectory(Path dir) {
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
