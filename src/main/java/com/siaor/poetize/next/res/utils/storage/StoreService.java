package com.siaor.poetize.next.res.utils.storage;

import com.siaor.poetize.next.app.vo.FileVO;

import java.util.List;

/**
 * 储存服务
 */
public interface StoreService {

    void deleteFile(List<String> files);

    FileVO saveFile(FileVO fileVO);

    String getStoreName();
}
