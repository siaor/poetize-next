package com.siaor.poetize.next.res.websocket;

import com.siaor.poetize.next.repo.po.im.ChatUserGroupMessagePO;
import com.siaor.poetize.next.repo.po.im.ChatUserMessagePO;
import com.siaor.poetize.next.pow.im.ImChatUserGroupMessagePow;
import com.siaor.poetize.next.pow.im.ImChatUserMessagePow;
import com.siaor.poetize.next.res.utils.mail.MailSendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Component
@Slf4j
public class MessageCache {

    @Autowired
    private ImChatUserMessagePow imChatUserMessagePow;

    @Autowired
    private ImChatUserGroupMessagePow imChatUserGroupMessagePow;

    @Autowired
    private MailSendUtil mailSendUtil;

    private final List<ChatUserMessagePO> userMessage = new ArrayList<>();

    private final List<ChatUserGroupMessagePO> groupMessage = new ArrayList<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void putUserMessage(ChatUserMessagePO message) {
        readWriteLock.readLock().lock();
        try {
            userMessage.add(message);
        } finally {
            readWriteLock.readLock().unlock();
        }

        try {
            mailSendUtil.sendImMail(message);
        } catch (Exception e) {
            log.error("发送IM邮件失败：", e);
        }
    }

    public void putGroupMessage(ChatUserGroupMessagePO message) {
        readWriteLock.readLock().lock();
        try {
            groupMessage.add(message);
        } finally {
            readWriteLock.readLock().unlock();
        }

    }

    @Scheduled(fixedDelay = 5000)
    public void saveUserMessage() {
        readWriteLock.writeLock().lock();
        try {
            if (!CollectionUtils.isEmpty(userMessage)) {
                imChatUserMessagePow.saveBatch(userMessage);
                userMessage.clear();
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void saveGroupMessage() {
        readWriteLock.writeLock().lock();
        try {
            if (!CollectionUtils.isEmpty(groupMessage)) {
                imChatUserGroupMessagePow.saveBatch(groupMessage);
                groupMessage.clear();
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
