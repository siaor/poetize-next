package com.siaor.poetize.next.app.api.im;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.po.im.ChatUserMessagePO;
import com.siaor.poetize.next.pow.im.ImChatUserMessagePow;
import com.siaor.poetize.next.app.vo.im.UserMessageVO;
import com.siaor.poetize.next.res.norm.im.ImConfigConst;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 单聊记录 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@RestController
@RequestMapping("/imChatUserMessage")
public class ImChatUserMessageApi {

    @Autowired
    private ImChatUserMessagePow imChatUserMessagePow;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 获取系统消息（只获取前十条）
     */
    @GetMapping("/listSystemMessage")
    @LoginCheck
    public ActResult<Page> listSystemMessage(@RequestParam(value = "current", defaultValue = "1") Long current,
                                             @RequestParam(value = "size", defaultValue = "10") Long size) {
        Page<ChatUserMessagePO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        LambdaQueryChainWrapper<ChatUserMessagePO> lambdaQuery = imChatUserMessagePow.lambdaQuery();
        lambdaQuery.select(ChatUserMessagePO::getId, ChatUserMessagePO::getContent, ChatUserMessagePO::getCreateTime);

        lambdaQuery.eq(ChatUserMessagePO::getFromId, ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        lambdaQuery.eq(ChatUserMessagePO::getToId, ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        lambdaQuery.orderByDesc(ChatUserMessagePO::getCreateTime);
        Page<ChatUserMessagePO> result = lambdaQuery.page(page);
        List<ChatUserMessagePO> records = result.getRecords();
        Collections.reverse(records);
        if (CollectionUtils.isEmpty(records)) {
            return ActResult.success(result);
        } else {
            List<UserMessageVO> collect = records.stream().map(message -> {
                UserMessageVO userMessageVO = new UserMessageVO();
                userMessageVO.setContent(message.getContent());
                userMessageVO.setId(message.getId());
                userMessageVO.setCreateTime(message.getCreateTime());
                return userMessageVO;
            }).collect(Collectors.toList());
            Page<UserMessageVO> resultVO = new Page<>();
            resultVO.setRecords(collect);
            resultVO.setTotal(result.getTotal());
            resultVO.setCurrent(result.getCurrent());
            resultVO.setSize(result.getSize());
            return ActResult.success(resultVO);
        }
    }


    /**
     * 管理员添加系统消息
     */
    @GetMapping("/saveSystemMessage")
    @LoginCheck(0)
    public ActResult saveSystemMessage(@RequestParam("content") String content) {
        ChatUserMessagePO userMessage = new ChatUserMessagePO();
        userMessage.setContent(content);
        userMessage.setFromId(ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        userMessage.setToId(ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_TRUE);
        imChatUserMessagePow.save(userMessage);
        return ActResult.success();
    }


    /**
     * 删除系统消息
     */
    @GetMapping("/deleteSystemMessage")
    @LoginCheck(0)
    public ActResult deleteSystemMessage(@RequestParam("id") Integer id) {
        imChatUserMessagePow.removeById(id);
        return ActResult.success();
    }

    /**
     * 获取朋友消息（只获取前四十条）
     */
    @GetMapping("/listFriendMessage")
    @LoginCheck
    public ActResult<Page> listFriendMessage(@RequestParam(value = "current", defaultValue = "1") Long current,
                                             @RequestParam(value = "size", defaultValue = "40") Long size,
                                             @RequestParam(value = "friendId") Integer friendId) {
        Page<ChatUserMessagePO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        Integer userId = PoetryUtil.getUserId();

        LambdaQueryChainWrapper<ChatUserMessagePO> lambdaQuery = imChatUserMessagePow.lambdaQuery();
        lambdaQuery.and(wrapper -> wrapper.eq(ChatUserMessagePO::getFromId, userId).eq(ChatUserMessagePO::getToId, friendId))
                .or(wrapper -> wrapper.eq(ChatUserMessagePO::getFromId, friendId).eq(ChatUserMessagePO::getToId, userId));
        lambdaQuery.orderByDesc(ChatUserMessagePO::getCreateTime);
        Page<ChatUserMessagePO> result = lambdaQuery.page(page);
        List<ChatUserMessagePO> records = result.getRecords();
        Collections.reverse(records);
        if (CollectionUtils.isEmpty(records)) {
            return ActResult.success(result);
        } else {
            List<UserMessageVO> collect = records.stream().map(message -> {
                UserMessageVO userMessageVO = new UserMessageVO();
                userMessageVO.setContent(message.getContent());
                userMessageVO.setFromId(message.getFromId());
                userMessageVO.setToId(message.getToId());
                userMessageVO.setMessageStatus(message.getMessageStatus());
                userMessageVO.setId(message.getId());
                userMessageVO.setCreateTime(message.getCreateTime());
                UserPO from = commonQuery.getUser(message.getFromId());
                if (from != null) {
                    userMessageVO.setAvatar(from.getAvatar());
                }
                return userMessageVO;
            }).collect(Collectors.toList());
            Page<UserMessageVO> resultVO = new Page<>();
            resultVO.setRecords(collect);
            resultVO.setTotal(result.getTotal());
            resultVO.setCurrent(result.getCurrent());
            resultVO.setSize(result.getSize());
            return ActResult.success(resultVO);
        }
    }
}

