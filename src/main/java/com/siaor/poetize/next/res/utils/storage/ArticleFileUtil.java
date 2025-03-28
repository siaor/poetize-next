package com.siaor.poetize.next.res.utils.storage;

import com.siaor.poetize.next.res.repo.po.ArticlePO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章文件工具类
 *
 * @author Siaor
 * @since 2025-02-23 03:58:04
 */
@Slf4j
@Component
public class ArticleFileUtil {

    @Value("${local.articlePath}")
    private String articlePath;

    @PostConstruct
    public void init() {
        this.articlePath = this.articlePath.replace("{root}", System.getProperty("user.dir"));
    }

    /**
     * 创建文章md文件
     *
     * @author Siaor
     * @since 2025-02-23 05:07:53
     */
    public void create(ArticlePO articlePO) {
        if (articlePO == null || articlePO.getId() == null || !StringUtils.hasText(articlePath)) {
            log.error("创建文章文件失败");
            return;
        }

        String fileName = articlePO.getId() + "." + articlePO.getArticleTitle() + ".md";
        String filePath = articlePath + articlePO.getUserId() + FileSystems.getDefault().getSeparator() + fileName;
        filePath = filePath.replaceAll("\\\\", "/");
        String fileContent = articlePO.getArticleContent();
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
    public void update(ArticlePO articlePO) {
        delete(articlePO.getUserId(), articlePO.getId(), articlePO.getArticleTitle());
        create(articlePO);
    }

    /**
     * 更新文章名称
     *
     * @author Siaor
     * @since 2025-03-10 08:29:08
     */
    public void appendId(String filePath, Integer id) {
        Path originalPath = Paths.get(filePath);
        Path parent = originalPath.getParent();
        Path newPath = parent.resolve(id + "." + originalPath.getFileName());

        try {
            Files.move(originalPath, newPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
            String filePath = articlePath + userId + FileSystems.getDefault().getSeparator() + fileName;
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
        String filePath = articlePath + userId + FileSystems.getDefault().getSeparator();
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

    /**
     * 读取文章文件
     *
     * @author Siaor
     * @since 2025-03-10 08:10:46
     */
    public ArticlePO read(String filePath) {
        File file = new File(filePath);
        if (!file.isFile() || !file.exists()) {
            return null;
        }

        ArticlePO articlePO = new ArticlePO();
        articlePO.setArticleCover("");
        articlePO.setVideoUrl("");
        articlePO.setPassword("");
        articlePO.setTips("");
        articlePO.setViewStatus(true);
        articlePO.setCommentStatus(true);
        articlePO.setRecommendStatus(false);
        articlePO.setSortId(1);
        articlePO.setLabelId(1);

        //名称
        String fileName = file.getName().replace(".md", "");
        articlePO.setArticleTitle(fileName);

        //ID
        String[] fileParts = fileName.split("\\.");
        if (fileParts.length >= 2) {
            try {
                Integer id = Integer.parseInt(fileParts[0]);
                articlePO.setId(id);
            } catch (NumberFormatException e) {
                log.info("未检测到ID");
            }
        }

        //内容
        Path path = Paths.get(filePath);
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            articlePO.setArticleContent(content);

            //读取第一张图片
            String regex = "\\!\\[.*?\\]\\((.*?)\\)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                articlePO.setArticleCover(matcher.group(1));
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }

        return articlePO;
    }

    /**
     * 获取用户文章文件列表
     *
     * @author Siaor
     * @since 2025-03-10 07:31:03
     */
    public List<String> list(Integer userId) {
        List<String> fileList = new ArrayList<>();
        if (userId == null) {
            return fileList;
        }
        String userArticleFilePath = articlePath + userId + FileSystems.getDefault().getSeparator();


        File directory = new File(userArticleFilePath);

        if (!directory.exists() || !directory.isDirectory()) {
            return fileList;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return fileList;
        }

        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".md")) {
                continue;
            }
            fileList.add(file.getAbsolutePath());
        }

        return fileList;
    }

}
