package com.siaor.poetize.next.app.api.sys;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.ResourcePO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.norm.SysEnum;
import com.siaor.poetize.next.pow.ResourcePow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.storage.StoreService;
import com.siaor.poetize.next.res.utils.storage.FileStorageService;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.FileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源信息 前端控制器
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@RestController
@RequestMapping("/resource")
public class ResourceApi {

    @Autowired
    private ResourcePow resourcePow;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 保存
     */
    @PostMapping("/saveResource")
    @LoginCheck
    public ActResult saveResource(@RequestBody ResourcePO resourcePO) {
        if (!StringUtils.hasText(resourcePO.getType()) || !StringUtils.hasText(resourcePO.getPath())) {
            return ActResult.fail("资源类型和资源路径不能为空！");
        }
        ResourcePO re = new ResourcePO();
        re.setPath(resourcePO.getPath());
        re.setType(resourcePO.getType());
        re.setSize(resourcePO.getSize());
        re.setOriginalName(resourcePO.getOriginalName());
        re.setMimeType(resourcePO.getMimeType());
        re.setStoreType(resourcePO.getStoreType());
        re.setUserId(PoetryUtil.getUserId());
        resourcePow.save(re);
        return ActResult.success();
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @LoginCheck
    public ActResult<String> upload(@RequestParam("file") MultipartFile file, FileVO fileVO) {
        if (file == null || !StringUtils.hasText(fileVO.getType()) || !StringUtils.hasText(fileVO.getRelativePath())) {
            return ActResult.fail("文件和资源类型和资源路径不能为空！");
        }

        fileVO.setFile(file);
        StoreService storeService = fileStorageService.getFileStorage(fileVO.getStoreType());
        FileVO result = storeService.saveFile(fileVO);

        ResourcePO re = new ResourcePO();
        re.setPath(result.getVisitPath());
        re.setType(fileVO.getType());
        re.setSize(Integer.valueOf(Long.toString(file.getSize())));
        re.setMimeType(file.getContentType());
        re.setStoreType(fileVO.getStoreType());
        re.setOriginalName(fileVO.getOriginalName());
        re.setUserId(PoetryUtil.getUserId());
        resourcePow.save(re);
        return ActResult.success(result.getVisitPath());
    }

    /**
     * 删除
     */
    @PostMapping("/deleteResource")
    @LoginCheck(0)
    public ActResult deleteResource(@RequestParam("path") String path) {
        ResourcePO resourcePO = resourcePow.lambdaQuery().select(ResourcePO::getStoreType).eq(ResourcePO::getPath, path).one();
        if (resourcePO == null) {
            return ActResult.fail("文件不存在：" + path);
        }

        StoreService storeService = fileStorageService.getFileStorageByStoreType(resourcePO.getStoreType());
        storeService.deleteFile(Collections.singletonList(path));
        return ActResult.success();
    }

    /**
     * 查询表情包
     */
    @GetMapping("/getImageList")
    @LoginCheck
    public ActResult<List<String>> getImageList() {
        List<ResourcePO> list = resourcePow.lambdaQuery().select(ResourcePO::getPath)
                .eq(ResourcePO::getType, CommonConst.PATH_TYPE_INTERNET_MEME)
                .eq(ResourcePO::getStatus, SysEnum.STATUS_ENABLE.getCode())
                .eq(ResourcePO::getUserId, PoetryUtil.getAdminUser().getId())
                .orderByDesc(ResourcePO::getCreateTime)
                .list();
        List<String> paths = list.stream().map(ResourcePO::getPath).collect(Collectors.toList());
        return ActResult.success(paths);
    }

    /**
     * 查询资源
     */
    @PostMapping("/listResource")
    @LoginCheck(0)
    public ActResult<Page> listResource(@RequestBody BaseRequestVO baseRequestVO) {
        resourcePow.lambdaQuery()
                .eq(StringUtils.hasText(baseRequestVO.getResourceType()), ResourcePO::getType, baseRequestVO.getResourceType())
                .orderByDesc(ResourcePO::getCreateTime).page(baseRequestVO);
        return ActResult.success(baseRequestVO);
    }

    /**
     * 修改资源状态
     */
    @GetMapping("/changeResourceStatus")
    @LoginCheck(0)
    public ActResult changeResourceStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        resourcePow.lambdaUpdate().eq(ResourcePO::getId, id).set(ResourcePO::getStatus, flag).update();
        return ActResult.success();
    }
}

