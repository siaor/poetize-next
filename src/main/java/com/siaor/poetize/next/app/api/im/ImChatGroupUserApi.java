package com.siaor.poetize.next.app.api.im;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.po.im.ChatGroupPO;
import com.siaor.poetize.next.res.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.pow.im.ImChatGroupPow;
import com.siaor.poetize.next.pow.im.ImChatGroupUserPow;
import com.siaor.poetize.next.app.vo.im.GroupUserVO;
import com.siaor.poetize.next.res.norm.im.ImConfigConst;
import com.siaor.poetize.next.res.utils.TioUtil;
import com.siaor.poetize.next.res.oper.im.TioWebsocketStarter;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tio.core.Tio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 聊天群成员 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@RestController
@RequestMapping("/imChatGroupUser")
public class ImChatGroupUserApi {

    @Autowired
    private ImChatGroupPow imChatGroupPow;

    @Autowired
    private ImChatGroupUserPow imChatGroupUserPow;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 申请加群
     */
    @GetMapping("/enterGroup")
    @LoginCheck
    public ActResult enterGroup(@RequestParam("id") Integer id, @RequestParam(value = "remark", required = false) String remark) {
        ChatGroupPO chatGroup = imChatGroupPow.getById(id);
        if (chatGroup == null) {
            return ActResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
            return ActResult.fail("话题无需申请！");
        }

        Integer userId = PoetryUtil.getUserId();

        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        lambdaQuery.eq(ChatGroupUserPO::getGroupId, id);
        lambdaQuery.eq(ChatGroupUserPO::getUserId, userId);
        ChatGroupUserPO groupUser = lambdaQuery.one();
        if (groupUser != null) {
            return ActResult.fail("重复申请！");
        }

        ChatGroupUserPO chatGroupUserPO = new ChatGroupUserPO();
        chatGroupUserPO.setGroupId(id);
        chatGroupUserPO.setUserId(userId);
        if (StringUtils.hasText(remark)) {
            chatGroupUserPO.setRemark(remark);
        }
        if (chatGroup.getInType() == ImConfigConst.IN_TYPE_TRUE) {
            chatGroupUserPO.setUserStatus(ImConfigConst.GROUP_USER_STATUS_NOT_VERIFY);
        } else {
            chatGroupUserPO.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        }
        boolean isSuccess = imChatGroupUserPow.save(chatGroupUserPO);
        if (isSuccess && chatGroup.getInType() == ImConfigConst.IN_TYPE_FALSE) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(id));
            }
        }
        return ActResult.success();
    }

    /**
     * 改变群组用户状态
     * <p>
     * 用户状态[-1:审核不通过或者踢出群组，1:审核通过，2:禁言]
     */
    @GetMapping("/changeUserStatus")
    @LoginCheck
    public ActResult changeUserStatus(@RequestParam("groupId") Integer groupId,
                                      @RequestParam("userId") Integer userId,
                                      @RequestParam("userStatus") Integer userStatus,
                                      @RequestParam("oldUserStatus") Integer oldUserStatus) {
        ChatGroupPO chatGroup = imChatGroupPow.getById(groupId);
        if (chatGroup == null) {
            return ActResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
            return ActResult.fail("话题无需操作！");
        }

        Integer currentUserId = PoetryUtil.getUserId();
        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        lambdaQuery.eq(ChatGroupUserPO::getGroupId, groupId);
        lambdaQuery.eq(ChatGroupUserPO::getUserId, currentUserId);
        lambdaQuery.eq(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_TRUE);
        ChatGroupUserPO groupUser = lambdaQuery.one();
        if (groupUser == null) {
            return ActResult.fail("没有审核权限！");
        }

        boolean isSuccess;
        if (userStatus == ImConfigConst.GROUP_USER_STATUS_BAN) {
            LambdaUpdateChainWrapper<ChatGroupUserPO> lambdaUpdate = imChatGroupUserPow.lambdaUpdate();
            lambdaUpdate.eq(ChatGroupUserPO::getGroupId, groupId);
            lambdaUpdate.eq(ChatGroupUserPO::getUserId, userId);
            lambdaUpdate.eq(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_FALSE);
            lambdaUpdate.eq(ChatGroupUserPO::getUserStatus, oldUserStatus);
            isSuccess = lambdaUpdate.remove();
        } else {
            LambdaUpdateChainWrapper<ChatGroupUserPO> lambdaUpdate = imChatGroupUserPow.lambdaUpdate();
            lambdaUpdate.eq(ChatGroupUserPO::getGroupId, groupId);
            lambdaUpdate.eq(ChatGroupUserPO::getUserId, userId);
            lambdaUpdate.eq(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_FALSE);
            lambdaUpdate.eq(ChatGroupUserPO::getUserStatus, oldUserStatus);
            lambdaUpdate.set(ChatGroupUserPO::getUserStatus, userStatus);
            if (userStatus == ImConfigConst.GROUP_USER_STATUS_PASS) {
                lambdaUpdate.set(ChatGroupUserPO::getVerifyUserId, currentUserId);
            }
            isSuccess = lambdaUpdate.update();
        }
        if (isSuccess && userStatus == ImConfigConst.GROUP_USER_STATUS_PASS) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(groupId));
            }
        } else if (isSuccess && userStatus.intValue() == ImConfigConst.GROUP_USER_STATUS_BAN &&
                (oldUserStatus.intValue() == ImConfigConst.GROUP_USER_STATUS_PASS ||
                        oldUserStatus.intValue() == ImConfigConst.GROUP_USER_STATUS_SILENCE)) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.unbindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(groupId));
            }
        }

        if (isSuccess) {
            return ActResult.success();
        } else {
            return ActResult.fail("修改失败！");
        }
    }

    /**
     * 设置群组管理员
     * <p>
     * adminFlag = true 是管理员
     * adminFlag = false 不是管理员
     */
    @GetMapping("/changeAdmin")
    @LoginCheck
    public ActResult changeAdmin(@RequestParam("groupId") Integer groupId,
                                 @RequestParam("userId") Integer userId,
                                 @RequestParam("adminFlag") Boolean adminFlag) {
        ChatGroupPO chatGroup = imChatGroupPow.getById(groupId);
        if (chatGroup == null) {
            return ActResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
            return ActResult.fail("话题无需操作！");
        }

        Integer currentUserId = PoetryUtil.getUserId();
        if (chatGroup.getMasterUserId().intValue() != currentUserId.intValue()) {
            return ActResult.fail("群主才能设置管理员！");
        }

        LambdaUpdateChainWrapper<ChatGroupUserPO> lambdaUpdate = imChatGroupUserPow.lambdaUpdate();
        lambdaUpdate.eq(ChatGroupUserPO::getGroupId, groupId);
        lambdaUpdate.eq(ChatGroupUserPO::getUserId, userId);
        lambdaUpdate.set(ChatGroupUserPO::getAdminFlag, adminFlag);

        lambdaUpdate.update();
        return ActResult.success();
    }

    /**
     * 退群
     */
    @GetMapping("/quitGroup")
    @LoginCheck
    public ActResult quitGroup(@RequestParam("id") Integer id) {
        ChatGroupPO chatGroup = imChatGroupPow.getById(id);
        if (chatGroup == null) {
            return ActResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
            return ActResult.fail("话题无需操作！");
        }

        Integer userId = PoetryUtil.getUserId();

        if (chatGroup.getMasterUserId().intValue() == userId.intValue()) {
            //转让群
            LambdaQueryChainWrapper<ChatGroupUserPO> wrapper = imChatGroupUserPow.lambdaQuery();
            wrapper.ne(ChatGroupUserPO::getUserId, userId);
            wrapper.last("order by admin_flag desc, create_time asc limit 1");
            ChatGroupUserPO one = wrapper.one();

            if (one == null) {
                //除了群主没别人，直接删除
                imChatGroupPow.removeById(id);
            } else {
                LambdaUpdateChainWrapper<ChatGroupPO> groupUpdate = imChatGroupPow.lambdaUpdate();
                groupUpdate.eq(ChatGroupPO::getId, id);
                groupUpdate.set(ChatGroupPO::getMasterUserId, one.getUserId());
                groupUpdate.update();
                LambdaUpdateChainWrapper<ChatGroupUserPO> groupUserUpdate = imChatGroupUserPow.lambdaUpdate();
                groupUserUpdate.eq(ChatGroupUserPO::getId, one.getId());
                groupUserUpdate.set(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_TRUE);
                groupUserUpdate.update();
            }
        }

        LambdaUpdateChainWrapper<ChatGroupUserPO> lambdaUpdate = imChatGroupUserPow.lambdaUpdate();
        lambdaUpdate.eq(ChatGroupUserPO::getGroupId, id);
        lambdaUpdate.eq(ChatGroupUserPO::getUserId, userId);
        boolean isSuccess = lambdaUpdate.remove();
        if (isSuccess) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.unbindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(id));
            }
        }
        return ActResult.success();
    }

    /**
     * 群管理员查询群用户
     */
    @GetMapping("/getGroupUserByStatus")
    @LoginCheck
    public ActResult<Page> getGroupUserByStatus(@RequestParam(value = "groupId", required = false) Integer groupId,
                                                @RequestParam(value = "userStatus", required = false) Integer userStatus,
                                                @RequestParam(value = "current", defaultValue = "1") Long current,
                                                @RequestParam(value = "size", defaultValue = "20") Long size) {
        Integer userId = PoetryUtil.getUserId();
        Page<ChatGroupUserPO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        if (groupId != null) {
            ChatGroupPO chatGroup = imChatGroupPow.getById(groupId);
            if (chatGroup == null) {
                return ActResult.fail("群组不存在！");
            }

            if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
                return ActResult.fail("话题没有用户！");
            }

            LambdaQueryChainWrapper<ChatGroupUserPO> groupLambdaQuery = imChatGroupUserPow.lambdaQuery();
            groupLambdaQuery.eq(ChatGroupUserPO::getGroupId, groupId);
            groupLambdaQuery.eq(ChatGroupUserPO::getUserId, userId);
            groupLambdaQuery.eq(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_TRUE);
            ChatGroupUserPO groupUser = groupLambdaQuery.one();
            if (groupUser == null) {
                return ActResult.fail("没有审核权限！");
            }
            lambdaQuery.eq(ChatGroupUserPO::getGroupId, groupId);
        } else {
            LambdaQueryChainWrapper<ChatGroupUserPO> userLambdaQuery = imChatGroupUserPow.lambdaQuery();
            userLambdaQuery.eq(ChatGroupUserPO::getUserId, userId);
            userLambdaQuery.eq(ChatGroupUserPO::getAdminFlag, ImConfigConst.ADMIN_FLAG_TRUE);
            List<ChatGroupUserPO> groupUsers = userLambdaQuery.list();
            if (CollectionUtils.isEmpty(groupUsers)) {
                // 该用户没有管理任何群
                return ActResult.success();
            } else {
                List<Integer> groupIds = groupUsers.stream().map(ChatGroupUserPO::getGroupId).collect(Collectors.toList());
                lambdaQuery.in(ChatGroupUserPO::getGroupId, groupIds);
            }
        }

        if (userStatus != null) {
            lambdaQuery.eq(ChatGroupUserPO::getUserStatus, userStatus);
        }

        lambdaQuery.orderByDesc(ChatGroupUserPO::getCreateTime).page(page);

        List<GroupUserVO> groupUserVOList = new ArrayList<>(page.getRecords().size());
        List<ChatGroupUserPO> records = page.getRecords();
        Map<Integer, List<ChatGroupUserPO>> map = records.stream().collect(Collectors.groupingBy(ChatGroupUserPO::getGroupId));
        List<ChatGroupPO> groups = imChatGroupPow.lambdaQuery().select(ChatGroupPO::getId, ChatGroupPO::getGroupName).in(ChatGroupPO::getId, map.keySet()).list();
        Map<Integer, String> collect = groups.stream().collect(Collectors.toMap(ChatGroupPO::getId, ChatGroupPO::getGroupName));
        map.forEach((key, value) -> {
            String groupName = collect.get(key);
            value.forEach(g -> {
                GroupUserVO groupUserVO = new GroupUserVO();
                BeanUtils.copyProperties(g, groupUserVO);
                groupUserVO.setGroupName(groupName);
                UserPO userPO = commonQuery.getUser(groupUserVO.getUserId());
                if (userPO != null) {
                    groupUserVO.setUsername(userPO.getUsername());
                    groupUserVO.setAvatar(userPO.getAvatar());
                }
                groupUserVOList.add(groupUserVO);
            });
        });

        Page<GroupUserVO> result = new Page<>();
        result.setRecords(groupUserVOList);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return ActResult.success(result);
    }

    /**
     * 群用户查询群用户
     */
    @GetMapping("/getGroupUser")
    @LoginCheck
    public ActResult<Page> getGroupUser(@RequestParam("groupId") Integer groupId,
                                        @RequestParam(value = "current", defaultValue = "1") Long current,
                                        @RequestParam(value = "size", defaultValue = "20") Long size) {
        ChatGroupPO chatGroup = imChatGroupPow.getById(groupId);
        if (chatGroup == null) {
            return ActResult.fail("群组不存在！");
        }

        if (chatGroup.getGroupType().intValue() == ImConfigConst.GROUP_TOPIC) {
            return ActResult.fail("话题没有用户！");
        }

        Integer userId = PoetryUtil.getUserId();
        LambdaQueryChainWrapper<ChatGroupUserPO> wrapper = imChatGroupUserPow.lambdaQuery();
        wrapper.eq(ChatGroupUserPO::getUserId, userId);
        wrapper.eq(ChatGroupUserPO::getGroupId, groupId);
        wrapper.in(ChatGroupUserPO::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        Integer count = wrapper.count().intValue();
        if (count < 1) {
            return ActResult.fail("未加群！");
        }

        Page<ChatGroupUserPO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        lambdaQuery.eq(ChatGroupUserPO::getGroupId, groupId);
        lambdaQuery.in(ChatGroupUserPO::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        lambdaQuery.orderByAsc(ChatGroupUserPO::getCreateTime).page(page);

        List<GroupUserVO> groupUserVOList = new ArrayList<>(page.getRecords().size());
        List<ChatGroupUserPO> records = page.getRecords();
        records.forEach(g -> {
            GroupUserVO groupUserVO = new GroupUserVO();
            BeanUtils.copyProperties(g, groupUserVO);
            groupUserVO.setGroupName(chatGroup.getGroupName());
            UserPO userPO = commonQuery.getUser(groupUserVO.getUserId());
            if (userPO != null) {
                groupUserVO.setUsername(userPO.getUsername());
                groupUserVO.setAvatar(userPO.getAvatar());
            }
            groupUserVOList.add(groupUserVO);
        });

        Page<GroupUserVO> result = new Page<>();
        result.setRecords(groupUserVOList);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return ActResult.success(result);
    }
}

