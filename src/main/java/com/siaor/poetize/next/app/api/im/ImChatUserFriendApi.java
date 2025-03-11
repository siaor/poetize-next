package com.siaor.poetize.next.app.api.im;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.repo.po.im.ChatUserFriendPO;
import com.siaor.poetize.next.pow.im.ImChatUserFriendPow;
import com.siaor.poetize.next.app.vo.im.UserFriendVO;
import com.siaor.poetize.next.res.websocket.ImConfigConst;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 好友 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@RestController
@RequestMapping("/imChatUserFriend")
public class ImChatUserFriendApi {

    @Autowired
    private ImChatUserFriendPow userFriendService;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 添加好友申请
     */
    @GetMapping("/addFriend")
    @LoginCheck
    public PoetryResult addFriend(@RequestParam("friendId") Integer friendId, @RequestParam(value = "remark", required = false) String remark) {
        UserPO friend = commonQuery.getUser(friendId);
        if (friend == null) {
            return PoetryResult.fail("用户不存在！");
        }

        Integer userId = PoetryUtil.getUserId();

        Integer count = userFriendService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ChatUserFriendPO::getUserId, userId).eq(ChatUserFriendPO::getFriendId, friendId))
                .or(wrapper -> wrapper.eq(ChatUserFriendPO::getFriendId, userId).eq(ChatUserFriendPO::getUserId, friendId))
                .count().intValue();
        if (count > 0) {
            return PoetryResult.success();
        }

        ChatUserFriendPO imChatFriend = new ChatUserFriendPO();
        imChatFriend.setUserId(friendId);
        imChatFriend.setFriendId(userId);
        imChatFriend.setFriendStatus(ImConfigConst.FRIEND_STATUS_NOT_VERIFY);
        imChatFriend.setRemark(remark);
        userFriendService.save(imChatFriend);
        return PoetryResult.success();
    }

    /**
     * 查询好友
     */
    @GetMapping("/getFriend")
    @LoginCheck
    public PoetryResult<List<UserFriendVO>> getFriend(@RequestParam(value = "friendStatus", required = false) Integer friendStatus) {
        Integer userId = PoetryUtil.getUserId();
        LambdaQueryChainWrapper<ChatUserFriendPO> wrapper = userFriendService.lambdaQuery().eq(ChatUserFriendPO::getUserId, userId);
        if (friendStatus != null) {
            wrapper.eq(ChatUserFriendPO::getFriendStatus, friendStatus);
        }

        List<ChatUserFriendPO> userFriends = wrapper.orderByDesc(ChatUserFriendPO::getCreateTime).list();
        List<UserFriendVO> userFriendVOS = new ArrayList<>(userFriends.size());
        userFriends.forEach(userFriend -> {
            UserPO friend = commonQuery.getUser(userFriend.getFriendId());
            if (friend != null) {
                UserFriendVO userFriendVO = new UserFriendVO();
                userFriendVO.setId(userFriend.getId());
                userFriendVO.setUserId(userFriend.getUserId());
                userFriendVO.setFriendId(userFriend.getFriendId());
                userFriendVO.setCreateTime(userFriend.getCreateTime());
                userFriendVO.setRemark(StringUtils.hasText(userFriend.getRemark()) ? userFriend.getRemark() : friend.getUsername());
                userFriendVO.setFriendStatus(userFriend.getFriendStatus());
                userFriendVO.setUsername(friend.getUsername());
                userFriendVO.setAvatar(friend.getAvatar());
                userFriendVO.setGender(friend.getGender());
                userFriendVO.setIntroduction(friend.getIntroduction());
                userFriendVOS.add(userFriendVO);
            }
        });
        return PoetryResult.success(userFriendVOS);
    }

    /**
     * 修改好友
     * <p>
     * 朋友状态[-1:审核不通过或者删除好友，0:未审核，1:审核通过]
     */
    @GetMapping("/changeFriend")
    @LoginCheck
    public PoetryResult changeFriend(@RequestParam("friendId") Integer friendId,
                                     @RequestParam(value = "friendStatus", required = false) Integer friendStatus,
                                     @RequestParam(value = "remark", required = false) String remark) {
        Integer userId = PoetryUtil.getUserId();
        ChatUserFriendPO userFriend = userFriendService.lambdaQuery()
                .eq(ChatUserFriendPO::getUserId, userId)
                .eq(ChatUserFriendPO::getFriendId, friendId).one();

        if (userFriend == null) {
            return PoetryResult.fail("好友不存在！");
        }

        if (friendStatus != null) {
            if (friendStatus == ImConfigConst.FRIEND_STATUS_PASS) {
                userFriendService.lambdaUpdate()
                        .set(ChatUserFriendPO::getFriendStatus, friendStatus)
                        .eq(ChatUserFriendPO::getId, userFriend.getId()).update();

                ChatUserFriendPO imChatFriend = new ChatUserFriendPO();
                imChatFriend.setUserId(friendId);
                imChatFriend.setFriendId(userId);
                imChatFriend.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
                userFriendService.save(imChatFriend);
            }

            if (friendStatus == ImConfigConst.FRIEND_STATUS_BAN) {
                userFriendService.removeById(userFriend.getId());
                userFriendService.lambdaUpdate()
                        .eq(ChatUserFriendPO::getUserId, friendId)
                        .eq(ChatUserFriendPO::getFriendId, userId).remove();
            }
        }

        if (StringUtils.hasText(remark)) {
            userFriendService.lambdaUpdate()
                    .set(ChatUserFriendPO::getRemark, remark)
                    .eq(ChatUserFriendPO::getId, userFriend.getId()).update();
        }


        return PoetryResult.success();
    }
}

