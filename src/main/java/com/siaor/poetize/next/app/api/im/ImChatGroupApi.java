package com.siaor.poetize.next.app.api.im;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.aop.SaveCheck;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.repo.po.im.ChatGroupPO;
import com.siaor.poetize.next.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.repo.po.im.ChatUserGroupMessagePO;
import com.siaor.poetize.next.pow.im.ImChatGroupPow;
import com.siaor.poetize.next.pow.im.ImChatGroupUserPow;
import com.siaor.poetize.next.pow.im.ImChatUserGroupMessagePow;
import com.siaor.poetize.next.app.vo.im.GroupVO;
import com.siaor.poetize.next.res.websocket.ImConfigConst;
import com.siaor.poetize.next.res.websocket.TioUtil;
import com.siaor.poetize.next.res.websocket.TioWebsocketStarter;
import com.siaor.poetize.next.res.enums.CodeMsg;
import com.siaor.poetize.next.res.enums.SysEnum;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.tio.core.Tio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 聊天群 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@RestController
@RequestMapping("/imChatGroup")
public class ImChatGroupApi {

    @Autowired
    private ImChatGroupPow imChatGroupPow;

    @Autowired
    private ImChatGroupUserPow imChatGroupUserPow;

    @Autowired
    private ImChatUserGroupMessagePow imChatUserGroupMessagePow;

    /**
     * 创建群组
     */
    @PostMapping("/creatGroupCommon")
    @LoginCheck
    @SaveCheck
    public PoetryResult creatGroup(@RequestBody ChatGroupPO chatGroupPO) {
        if (!StringUtils.hasText(chatGroupPO.getGroupName())) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        chatGroupPO.setGroupType(ImConfigConst.GROUP_COMMON);
        Integer userId = PoetryUtil.getUserId();
        chatGroupPO.setMasterUserId(userId);
        imChatGroupPow.save(chatGroupPO);

        ChatGroupUserPO chatGroupUserPO = new ChatGroupUserPO();
        chatGroupUserPO.setGroupId(chatGroupPO.getId());
        chatGroupUserPO.setUserId(userId);
        chatGroupUserPO.setAdminFlag(ImConfigConst.ADMIN_FLAG_TRUE);
        chatGroupUserPO.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        imChatGroupUserPow.save(chatGroupUserPO);

        TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
        if (tioWebsocketStarter != null) {
            Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(chatGroupPO.getId()));
        }

        return PoetryResult.success();
    }

    /**
     * 创建话题
     */
    @PostMapping("/creatGroupTopic")
    @LoginCheck(0)
    public PoetryResult creatGroupTopic(@RequestBody ChatGroupPO chatGroupPO) {
        if (!StringUtils.hasText(chatGroupPO.getGroupName())) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        chatGroupPO.setGroupType(ImConfigConst.GROUP_TOPIC);
        Integer userId = PoetryUtil.getUserId();
        chatGroupPO.setMasterUserId(userId);
        chatGroupPO.setInType(ImConfigConst.IN_TYPE_FALSE);
        imChatGroupPow.save(chatGroupPO);

        return PoetryResult.success();
    }


    /**
     * 更新组
     * <p>
     * 只有群主才能修改组
     */
    @PostMapping("/updateGroup")
    @LoginCheck
    public PoetryResult updateGroup(@RequestBody ChatGroupPO chatGroupPO) {
        LambdaUpdateChainWrapper<ChatGroupPO> lambdaUpdate = imChatGroupPow.lambdaUpdate();
        lambdaUpdate.eq(ChatGroupPO::getId, chatGroupPO.getId());
        lambdaUpdate.eq(ChatGroupPO::getMasterUserId, PoetryUtil.getUserId());
        if (StringUtils.hasText(chatGroupPO.getGroupName())) {
            lambdaUpdate.set(ChatGroupPO::getGroupName, chatGroupPO.getGroupName());
        }
        if (StringUtils.hasText(chatGroupPO.getAvatar())) {
            lambdaUpdate.set(ChatGroupPO::getAvatar, chatGroupPO.getAvatar());
        }
        if (StringUtils.hasText(chatGroupPO.getIntroduction())) {
            lambdaUpdate.set(ChatGroupPO::getIntroduction, chatGroupPO.getIntroduction());
        }
        // 群通知
        if (StringUtils.hasText(chatGroupPO.getNotice())) {
            lambdaUpdate.set(ChatGroupPO::getNotice, chatGroupPO.getNotice());
        }
        // 修改进入方式
        if (chatGroupPO.getInType() != null) {
            lambdaUpdate.set(ChatGroupPO::getInType, chatGroupPO.getInType());
        }
        // 转让群
        if (chatGroupPO.getMasterUserId() != null) {
            lambdaUpdate.set(ChatGroupPO::getMasterUserId, chatGroupPO.getMasterUserId());
        }
        boolean isSuccess = lambdaUpdate.update();
        if (isSuccess && StringUtils.hasText(chatGroupPO.getNotice())) {
            // todo 发送群公告
        }
        return PoetryResult.success();
    }

    /**
     * 解散群
     */
    @GetMapping("/deleteGroup")
    @LoginCheck
    public PoetryResult deleteGroup(@RequestParam("id") Integer id) {
        UserPO currentUserPO = PoetryUtil.getCurrentUser();
        boolean isSuccess;
        if (currentUserPO.getUserType().intValue() == SysEnum.USER_TYPE_ADMIN.getCode()) {
            isSuccess = imChatGroupPow.removeById(id);
        } else {
            LambdaUpdateChainWrapper<ChatGroupPO> lambdaUpdate = imChatGroupPow.lambdaUpdate();
            lambdaUpdate.eq(ChatGroupPO::getId, id);
            lambdaUpdate.eq(ChatGroupPO::getMasterUserId, PoetryUtil.getUserId());
            isSuccess = lambdaUpdate.remove();
        }
        if (isSuccess) {
            // 删除用户
            LambdaUpdateChainWrapper<ChatGroupUserPO> lambdaUpdate = imChatGroupUserPow.lambdaUpdate();
            lambdaUpdate.eq(ChatGroupUserPO::getGroupId, id).remove();
            // 删除聊天记录
            LambdaUpdateChainWrapper<ChatUserGroupMessagePO> messageLambdaUpdateChainWrapper = imChatUserGroupMessagePow.lambdaUpdate();
            messageLambdaUpdateChainWrapper.eq(ChatUserGroupMessagePO::getGroupId, id).remove();

            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.removeGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(id), "remove group");
            }
        }
        return PoetryResult.success();
    }

    /**
     * 管理员查询所有群
     */
    @PostMapping("/listGroupForAdmin")
    @LoginCheck(0)
    public PoetryResult<BaseRequestVO> listGroupForAdmin(@RequestBody BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<ChatGroupPO> lambdaQuery = imChatGroupPow.lambdaQuery();
        lambdaQuery.orderByDesc(ChatGroupPO::getCreateTime).page(baseRequestVO);
        return PoetryResult.success(baseRequestVO);
    }

    /**
     * 加入话题
     */
    @GetMapping("/addGroupTopic")
    @LoginCheck
    public PoetryResult addGroupTopic(@RequestParam("id") Integer id) {
        LambdaQueryChainWrapper<ChatGroupPO> lambdaQuery = imChatGroupPow.lambdaQuery();
        Integer count = lambdaQuery.eq(ChatGroupPO::getId, id)
                .eq(ChatGroupPO::getGroupType, ImConfigConst.GROUP_TOPIC).count().intValue();
        if (count == 1) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(PoetryUtil.getUserId()), String.valueOf(id));
            }
        }
        return PoetryResult.success();
    }

    /**
     * 用户查询所有群
     * <p>
     * 只查询审核通过和禁言的群
     */
    @GetMapping("/listGroup")
    @LoginCheck
    public PoetryResult<List<GroupVO>> listGroup() {
        Integer userId = PoetryUtil.getUserId();
        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        lambdaQuery.eq(ChatGroupUserPO::getUserId, userId);
        lambdaQuery.in(ChatGroupUserPO::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        List<ChatGroupUserPO> groupUsers = lambdaQuery.list();

        Map<Integer, ChatGroupUserPO> groupUserMap = groupUsers.stream().collect(Collectors.toMap(ChatGroupUserPO::getGroupId, Function.identity()));
        LambdaQueryChainWrapper<ChatGroupPO> wrapper = imChatGroupPow.lambdaQuery();
        wrapper.eq(ChatGroupPO::getGroupType, ImConfigConst.GROUP_TOPIC);
        if (!CollectionUtils.isEmpty(groupUserMap.keySet())) {
            wrapper.or(w -> w.in(ChatGroupPO::getId, groupUserMap.keySet())
                    .eq(ChatGroupPO::getGroupType, ImConfigConst.GROUP_COMMON));
        }
        List<ChatGroupPO> chatGroupPOS = wrapper.list();
        List<GroupVO> groupVOS = chatGroupPOS.stream().map(imChatGroup -> {
            ChatGroupUserPO chatGroupUserPO = groupUserMap.get(imChatGroup.getId());
            if (chatGroupUserPO == null) {
                chatGroupUserPO = new ChatGroupUserPO();
                chatGroupUserPO.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
                chatGroupUserPO.setCreateTime(LocalDateTime.now());
                chatGroupUserPO.setUserId(userId);
                chatGroupUserPO.setAdminFlag(userId.intValue() == PoetryUtil.getAdminUser().getId().intValue());
            }
            return getGroupVO(imChatGroup, chatGroupUserPO);
        }).collect(Collectors.toList());
        return PoetryResult.success(groupVOS);
    }

    private GroupVO getGroupVO(ChatGroupPO chatGroupPO, ChatGroupUserPO chatGroupUserPO) {
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupName(chatGroupPO.getGroupName());
        groupVO.setAvatar(chatGroupPO.getAvatar());
        groupVO.setIntroduction(chatGroupPO.getIntroduction());
        groupVO.setNotice(chatGroupPO.getNotice());
        groupVO.setInType(chatGroupPO.getInType());
        groupVO.setGroupType(chatGroupPO.getGroupType());
        groupVO.setId(chatGroupPO.getId());
        groupVO.setCreateTime(chatGroupUserPO.getCreateTime());
        groupVO.setUserStatus(chatGroupUserPO.getUserStatus());
        groupVO.setAdminFlag(chatGroupUserPO.getAdminFlag());
        groupVO.setMasterFlag(chatGroupPO.getMasterUserId().intValue() == chatGroupUserPO.getUserId().intValue());
        return groupVO;
    }
}

