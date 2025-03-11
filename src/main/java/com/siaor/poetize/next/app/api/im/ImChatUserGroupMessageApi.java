package com.siaor.poetize.next.app.api.im;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.repo.po.im.ChatGroupPO;
import com.siaor.poetize.next.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.repo.po.im.ChatUserGroupMessagePO;
import com.siaor.poetize.next.pow.im.ImChatGroupPow;
import com.siaor.poetize.next.pow.im.ImChatGroupUserPow;
import com.siaor.poetize.next.pow.im.ImChatUserGroupMessagePow;
import com.siaor.poetize.next.app.vo.im.GroupMessageVO;
import com.siaor.poetize.next.res.websocket.ImConfigConst;
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
 * 群聊记录 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@RestController
@RequestMapping("/imChatUserGroupMessage")
public class ImChatUserGroupMessageApi {

    @Autowired
    private ImChatUserGroupMessagePow imChatUserGroupMessagePow;

    @Autowired
    private ImChatGroupUserPow imChatGroupUserPow;

    @Autowired
    private ImChatGroupPow imChatGroupPow;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 获取群消息（只获取前四十条）
     */
    @GetMapping("/listGroupMessage")
    @LoginCheck
    public PoetryResult<Page> listGroupMessage(@RequestParam(value = "current", defaultValue = "1") Long current,
                                               @RequestParam(value = "size", defaultValue = "40") Long size,
                                               @RequestParam(value = "groupId") Integer groupId) {
        Page<ChatUserGroupMessagePO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        Integer userId = PoetryUtil.getUserId();

        ChatGroupPO chatGroup = imChatGroupPow.getById(groupId);
        if (chatGroup == null) {
            return PoetryResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_COMMON) {
            LambdaQueryChainWrapper<ChatGroupUserPO> groupLambdaQuery = imChatGroupUserPow.lambdaQuery();
            groupLambdaQuery.eq(ChatGroupUserPO::getGroupId, groupId);
            groupLambdaQuery.eq(ChatGroupUserPO::getUserId, userId);
            groupLambdaQuery.in(ChatGroupUserPO::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
            Integer count = groupLambdaQuery.count().intValue();
            if (count < 1) {
                return PoetryResult.fail("未加群！");
            }
        }

        LambdaQueryChainWrapper<ChatUserGroupMessagePO> lambdaQuery = imChatUserGroupMessagePow.lambdaQuery();
        lambdaQuery.eq(ChatUserGroupMessagePO::getGroupId, groupId);
        lambdaQuery.orderByDesc(ChatUserGroupMessagePO::getCreateTime);
        Page<ChatUserGroupMessagePO> result = lambdaQuery.page(page);
        List<ChatUserGroupMessagePO> records = result.getRecords();
        Collections.reverse(records);
        if (CollectionUtils.isEmpty(records)) {
            return PoetryResult.success(result);
        } else {
            List<GroupMessageVO> collect = records.stream().map(message -> {
                GroupMessageVO groupMessageVO = new GroupMessageVO();
                groupMessageVO.setContent(message.getContent());
                groupMessageVO.setFromId(message.getFromId());
                groupMessageVO.setToId(message.getToId());
                groupMessageVO.setId(message.getId());
                groupMessageVO.setGroupId(message.getGroupId());
                groupMessageVO.setCreateTime(message.getCreateTime());
                Integer messageUserId = message.getFromId();
                UserPO userPO = commonQuery.getUser(messageUserId);
                if (userPO != null) {
                    groupMessageVO.setUsername(userPO.getUsername());
                    groupMessageVO.setAvatar(userPO.getAvatar());
                }
                return groupMessageVO;
            }).collect(Collectors.toList());
            Page<GroupMessageVO> resultVO = new Page<>();
            resultVO.setRecords(collect);
            resultVO.setTotal(result.getTotal());
            resultVO.setCurrent(result.getCurrent());
            resultVO.setSize(result.getSize());
            return PoetryResult.success(resultVO);
        }
    }
}

