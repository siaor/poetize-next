package com.siaor.poetize.next.res.websocket;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.repo.po.im.ChatUserGroupMessagePO;
import com.siaor.poetize.next.repo.po.im.ChatUserMessagePO;
import com.siaor.poetize.next.pow.im.ImChatGroupUserPow;
import com.siaor.poetize.next.pow.im.ImChatUserMessagePow;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.res.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsResponse;
import org.tio.websocket.server.handler.IWsMsgHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ImWsMsgHandler implements IWsMsgHandler {

    @Autowired
    private ImChatGroupUserPow imChatGroupUserPow;

    @Autowired
    private ImChatUserMessagePow imChatUserMessagePow;

    @Autowired
    private MessageCache messageCache;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 握手时走这个方法，业务可以在这里获取cookie，request等
     * 对httpResponse参数进行补充并返回，如果返回null表示不想和对方建立连接
     * 对于大部分业务，该方法只需要一行代码：return httpResponse;
     */
    @Override
    public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
        String token = httpRequest.getParam(CommonConst.TOKEN_HEADER);

        if (!StringUtils.hasText(token)) {
            return null;
        }

        UserPO userPO = (UserPO) PoetryCache.get(token);

        if (userPO == null) {
            return null;
        }

        log.info("握手成功：用户ID：{}, 用户名：{}", userPO.getId(), userPO.getUsername());

        return httpResponse;
    }

    /**
     * 握手成功后触发该方法
     */
    @Override
    public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
        String token = httpRequest.getParam(CommonConst.TOKEN_HEADER);
        UserPO userPO = (UserPO) PoetryCache.get(token);
        Tio.closeUser(channelContext.tioConfig, userPO.getId().toString(), null);
        Tio.bindUser(channelContext, userPO.getId().toString());

        List<ChatUserMessagePO> userMessages = imChatUserMessagePow.lambdaQuery().eq(ChatUserMessagePO::getToId, userPO.getId())
                .eq(ChatUserMessagePO::getMessageStatus, ImConfigConst.USER_MESSAGE_STATUS_FALSE)
                .orderByAsc(ChatUserMessagePO::getCreateTime).list();

        if (!CollectionUtils.isEmpty(userMessages)) {
            List<Long> ids = new ArrayList<>();
            userMessages.forEach(userMessage -> {
                ids.add(userMessage.getId());
                ImMessage imMessage = new ImMessage();
                imMessage.setContent(userMessage.getContent());
                imMessage.setFromId(userMessage.getFromId());
                imMessage.setToId(userMessage.getToId());
                imMessage.setMessageType(ImEnum.MESSAGE_TYPE_MSG_SINGLE.getCode());
                UserPO friend = commonQuery.getUser(userMessage.getFromId());
                if (friend != null) {
                    imMessage.setAvatar(friend.getAvatar());
                }
                WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(imMessage), ImConfigConst.CHARSET);
                Tio.sendToUser(channelContext.tioConfig, userMessage.getToId().toString(), wsResponse);
            });
            imChatUserMessagePow.lambdaUpdate().in(ChatUserMessagePO::getId, ids)
                    .set(ChatUserMessagePO::getMessageStatus, ImConfigConst.USER_MESSAGE_STATUS_TRUE).update();

        }

        LambdaQueryChainWrapper<ChatGroupUserPO> lambdaQuery = imChatGroupUserPow.lambdaQuery();
        lambdaQuery.select(ChatGroupUserPO::getGroupId);
        lambdaQuery.eq(ChatGroupUserPO::getUserId, userPO.getId());
        lambdaQuery.in(ChatGroupUserPO::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        List<ChatGroupUserPO> groupUsers = lambdaQuery.list();
        if (!CollectionUtils.isEmpty(groupUsers)) {
            groupUsers.forEach(groupUser -> Tio.bindGroup(channelContext, groupUser.getGroupId().toString()));
        }
    }

    @Override
    public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        return null;
    }

    @Override
    public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        Tio.remove(channelContext, "连接关闭");
        return null;
    }

    @Override
    public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            ImMessage imMessage = JSON.parseObject(text, ImMessage.class);

            String content = StringUtil.removeHtml(imMessage.getContent());
            if (!StringUtils.hasText(content)) {
                return null;
            }
            imMessage.setContent(content);

            WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(imMessage), ImConfigConst.CHARSET);
            if (imMessage.getMessageType().intValue() == ImEnum.MESSAGE_TYPE_MSG_SINGLE.getCode()) {
                //单聊
                ChatUserMessagePO userMessage = new ChatUserMessagePO();
                userMessage.setFromId(imMessage.getFromId());
                userMessage.setToId(imMessage.getToId());
                userMessage.setContent(imMessage.getContent());
                userMessage.setCreateTime(LocalDateTime.now());

                SetWithLock<ChannelContext> setWithLock = Tio.getByUserid(channelContext.tioConfig, imMessage.getToId().toString());
                if (setWithLock != null && setWithLock.size() > 0) {
                    Tio.sendToUser(channelContext.tioConfig, imMessage.getToId().toString(), wsResponse);
                    userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_TRUE);
                } else {
                    userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_FALSE);
                }
                messageCache.putUserMessage(userMessage);
                Tio.sendToUser(channelContext.tioConfig, imMessage.getFromId().toString(), wsResponse);
            } else if (imMessage.getMessageType().intValue() == ImEnum.MESSAGE_TYPE_MSG_GROUP.getCode()) {
                //群聊
                ChatUserGroupMessagePO groupMessage = new ChatUserGroupMessagePO();
                groupMessage.setContent(imMessage.getContent());
                groupMessage.setFromId(imMessage.getFromId());
                groupMessage.setGroupId(imMessage.getGroupId());
                groupMessage.setCreateTime(LocalDateTime.now());
                messageCache.putGroupMessage(groupMessage);

                SetWithLock<ChannelContext> setWithLock = Tio.getByGroup(channelContext.tioConfig, imMessage.getGroupId().toString());
                if (setWithLock != null && setWithLock.size() > 0) {
                    Tio.sendToGroup(channelContext.tioConfig, imMessage.getGroupId().toString(), wsResponse);
                }
            }
        } catch (Exception e) {
            log.error("解析消息失败：{}", e.getMessage());
        }
        //返回值是要发送给客户端的内容，一般都是返回null
        return null;
    }
}
