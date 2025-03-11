package com.siaor.poetize.next.pro.im;

import com.siaor.poetize.next.repo.po.im.ChatUserMessagePO;
import com.siaor.poetize.next.repo.mapper.im.ImChatUserMessageMapper;
import com.siaor.poetize.next.pow.im.ImChatUserMessagePow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 单聊记录 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@Service
public class ImChatUserMessagePowPro extends ServiceImpl<ImChatUserMessageMapper, ChatUserMessagePO> implements ImChatUserMessagePow {

}
