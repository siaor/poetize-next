package com.siaor.poetize.next.res.utils.storage;

import cn.hutool.core.io.FileUtil;
import com.siaor.poetize.next.app.vo.FileVO;
import com.siaor.poetize.next.pow.ResourcePow;
import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import com.siaor.poetize.next.res.repo.po.ResourcePO;
import com.siaor.poetize.next.res.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "local.enable", havingValue = "true")
public class LocalUtil implements StoreService {

    private final String visitUrl = File.separator + "res" + File.separator;

    private final String resPath = System.getProperty("user.dir") + File.separator + "public" + visitUrl;

    @Autowired
    private ResourcePow resourcePow;

    @Override
    public void deleteFile(List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        for (String filePath : files) {
            File file = new File(filePath.replace(visitUrl, resPath));
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    log.info("文件删除成功：" + filePath);
                    resourcePow.lambdaUpdate().eq(ResourcePO::getPath, filePath).remove();
                } else {
                    log.error("文件删除失败：" + filePath);
                }
            } else {
                log.error("文件不存在或者不是一个文件：" + filePath);
            }
        }
    }

    @Override
    public FileVO saveFile(FileVO fileVO) {
        if (!StringUtils.hasText(fileVO.getRelativePath()) ||
                fileVO.getRelativePath().startsWith("/") ||
                fileVO.getRelativePath().endsWith("/")) {
            throw new SysRuntimeException("文件路径不合法！");
        }

        String path = fileVO.getRelativePath();
        if (path.contains("/")) {
            String[] split = path.split("/");
            if (split.length > 5) {
                throw new SysRuntimeException("文件路径不合法！");
            }
            for (int i = 0; i < split.length - 1; i++) {
                if (!StringUtil.isValidDirectoryName(split[i])) {
                    throw new SysRuntimeException("文件路径不合法！");
                }
            }
            if (!StringUtil.isValidFileName(split[split.length - 1])) {
                throw new SysRuntimeException("文件路径不合法！");
            }
        }
        String absolutePath = resPath + path;
        if (FileUtil.exist(absolutePath)) {
            throw new SysRuntimeException("文件已存在！");
        }
        try {
            File newFile = FileUtil.touch(absolutePath);
            fileVO.getFile().transferTo(newFile);
            FileVO result = new FileVO();
            result.setAbsolutePath(absolutePath);
            result.setVisitPath(visitUrl + path);
            return result;
        } catch (IOException e) {
            log.error("文件上传失败：", e);
            FileUtil.del(absolutePath);
            throw new SysRuntimeException("文件上传失败！");
        }
    }

    @Override
    public String getStoreName() {
        return StoreEnum.LOCAL.getCode();
    }
}
