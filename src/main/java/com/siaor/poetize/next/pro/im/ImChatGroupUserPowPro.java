package com.siaor.poetize.next.pro.im;

import com.siaor.poetize.next.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.repo.mapper.im.ImChatGroupUserMapper;
import com.siaor.poetize.next.pow.im.ImChatGroupUserPow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天群成员 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@Service
public class ImChatGroupUserPowPro extends ServiceImpl<ImChatGroupUserMapper, ChatGroupUserPO> implements ImChatGroupUserPow {

}
