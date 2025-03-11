package com.siaor.poetize.next.pro.im;

import com.siaor.poetize.next.res.repo.po.im.ChatUserGroupMessagePO;
import com.siaor.poetize.next.res.repo.mapper.im.ImChatUserGroupMessageMapper;
import com.siaor.poetize.next.pow.im.ImChatUserGroupMessagePow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 群聊记录 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@Service
public class ImChatUserGroupMessagePowPro extends ServiceImpl<ImChatUserGroupMessageMapper, ChatUserGroupMessagePO> implements ImChatUserGroupMessagePow {

}
