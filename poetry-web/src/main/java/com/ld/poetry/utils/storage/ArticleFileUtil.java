package com.ld.poetry.utils.storage;

import com.ld.poetry.entity.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * 文章文件工具类
 *
 * @author Siaor
 * @since 2025-02-23 03:58:04
 */
@Slf4j
@Component
public class ArticleFileUtil {

    @Value("${local.articleFilePath}")
    private String articleFilePath;

    /**
     * 创建文章md文件
     *
     * @author Siaor
     * @since 2025-02-23 05:07:53
     */
    public void create(Article article) {
        if (article == null || article.getId() == null || !StringUtils.hasText(articleFilePath)) {
            log.error("创建文章文件失败");
            return;
        }

        String fileName = article.getId() + "." + article.getArticleTitle() + ".md";
        String filePath = articleFilePath + article.getUserId() + FileSystems.getDefault().getSeparator() + fileName;
        filePath = filePath.replaceAll("\\\\", "/");
        String fileContent = article.getArticleContent();
        if (!StringUtils.hasText(fileContent)) {
            fileContent = "";
        }

        Path path = Paths.get(filePath);

        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                log.info("目录创建成功：{}", parentDir);
            } catch (IOException e) {
                log.error("创建目录失败：{}", parentDir, e);
                return;
            }
        }

        try {
            Files.write(path, fileContent.getBytes(StandardCharsets.UTF_8));
            log.info("文件创建成功：{}", filePath);
        } catch (IOException e) {
            log.error("写入文章内容失败：{}", filePath);
        }

    }

    /**
     * 更新文章md文件
     *
     * @author Siaor
     * @since 2025-02-23 05:07:30
     */
    public void update(Article article) {
        delete(article.getUserId(), article.getId(), article.getArticleTitle());
        create(article);
    }

    /**
     * 根据文章信息删除文件
     *
     * @author Siaor
     * @since 2025-02-23 05:06:23
     */
    public void delete(Integer userId, Integer id, String title) {
        if (userId == null || id == null) {
            return;
        }

        //根据名称直接删除
        if (StringUtils.hasText(title)) {
            String fileName = id + "." + title + ".md";
            String filePath = articleFilePath + userId + FileSystems.getDefault().getSeparator() + fileName;
            filePath = filePath.replaceAll("\\\\", "/");

            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    log.info("文件删除成功：{}", filePath);
                    return;
                }
                log.error("文件删除失败：{}", filePath);
            }
        }

        //根据ID删除
        delete(userId, id);
    }

    /**
     * 根据文章ID删除文件
     *
     * @author Siaor
     * @since 2025-02-23 05:06:59
     */
    public void delete(Integer userId, Integer id) {
        String filePath = articleFilePath + userId + FileSystems.getDefault().getSeparator();
        filePath = filePath.replaceAll("\\\\", "/");

        Path aPath = Paths.get(filePath);
        String filePre = id + ".";

        // 遍历目录中的文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(aPath, filePre + "*")) {
            Path aFile = null;
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    aFile = entry;
                    break;
                }
            }

            if (aFile != null) {
                Files.delete(aFile);
                log.info("文件删除成功：{}", id);
            } else {
                log.error("文件删除失败：{}", id);
            }
        } catch (IOException | DirectoryIteratorException e) {
            log.error(e.getMessage());
        }
    }

}
