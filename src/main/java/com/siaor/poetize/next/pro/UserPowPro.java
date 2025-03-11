package com.siaor.poetize.next.pro;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.UserMapper;
import com.siaor.poetize.next.res.repo.po.WebInfoPO;
import com.siaor.poetize.next.res.repo.po.WeiYanPO;
import com.siaor.poetize.next.res.norm.SysEnum;
import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import com.siaor.poetize.next.res.repo.mapper.im.ImChatGroupUserMapper;
import com.siaor.poetize.next.res.repo.mapper.im.ImChatUserFriendMapper;
import com.siaor.poetize.next.res.repo.po.im.ChatGroupUserPO;
import com.siaor.poetize.next.res.repo.po.im.ChatUserFriendPO;
import com.siaor.poetize.next.res.norm.im.ImConfigConst;
import com.siaor.poetize.next.res.utils.TioUtil;
import com.siaor.poetize.next.res.oper.im.TioWebsocketStarter;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.pow.WeiYanPow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.utils.mail.MailUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.tio.core.Tio;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@Service
@Slf4j
public class UserPowPro extends ServiceImpl<UserMapper, UserPO> implements UserPow {

    @Autowired
    private WeiYanPow weiYanPow;

    @Autowired
    private ImChatGroupUserMapper imChatGroupUserMapper;

    @Autowired
    private ImChatUserFriendMapper imChatUserFriendMapper;

    @Autowired
    private MailUtil mailUtil;

    @Value("${user.code.format}")
    private String codeFormat;

    @Override
    public ActResult<UserVO> login(String account, String password, Boolean isAdmin) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

        UserPO one = lambdaQuery().and(wrapper -> wrapper
                        .eq(UserPO::getUsername, account)
                        .or()
                        .eq(UserPO::getEmail, account)
                        .or()
                        .eq(UserPO::getPhoneNumber, account))
                .eq(UserPO::getPassword, DigestUtils.md5DigestAsHex(password.getBytes()))
                .one();

        if (one == null) {
            return ActResult.fail("账号/密码错误，请重新输入！");
        }

        if (!one.getUserStatus()) {
            return ActResult.fail("账号被冻结！");
        }

        String adminToken = "";
        String userToken = "";

        if (isAdmin) {
            if (one.getUserType() != SysEnum.USER_TYPE_ADMIN.getCode() && one.getUserType() != SysEnum.USER_TYPE_AUTH.getCode()) {
                return ActResult.fail("请输入管理员账号！");
            }
            if (SysCache.get(CommonConst.ADMIN_TOKEN + one.getId()) != null) {
                adminToken = (String) SysCache.get(CommonConst.ADMIN_TOKEN + one.getId());
            }
        } else {
            if (SysCache.get(CommonConst.USER_TOKEN + one.getId()) != null) {
                userToken = (String) SysCache.get(CommonConst.USER_TOKEN + one.getId());
            }
        }


        if (isAdmin && !StringUtils.hasText(adminToken)) {
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            adminToken = CommonConst.ADMIN_ACCESS_TOKEN + uuid;
            SysCache.put(adminToken, one, CommonConst.TOKEN_EXPIRE);
            SysCache.put(CommonConst.ADMIN_TOKEN + one.getId(), adminToken, CommonConst.TOKEN_EXPIRE);
        } else if (!isAdmin && !StringUtils.hasText(userToken)) {
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            userToken = CommonConst.USER_ACCESS_TOKEN + uuid;
            SysCache.put(userToken, one, CommonConst.TOKEN_EXPIRE);
            SysCache.put(CommonConst.USER_TOKEN + one.getId(), userToken, CommonConst.TOKEN_EXPIRE);
        }


        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        if (isAdmin && one.getUserType() == SysEnum.USER_TYPE_ADMIN.getCode()) {
            userVO.setIsBoss(true);
        }

        if (isAdmin) {
            userVO.setAccessToken(adminToken);
        } else {
            userVO.setAccessToken(userToken);
        }
        return ActResult.success(userVO);
    }

    @Override
    public ActResult exit() {
        String token = PoetryUtil.getToken();
        Integer userId = PoetryUtil.getUserId();
        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            SysCache.remove(CommonConst.USER_TOKEN + userId);
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
            }
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            SysCache.remove(CommonConst.ADMIN_TOKEN + userId);
        }
        SysCache.remove(token);
        return ActResult.success();
    }

    @Override
    public ActResult<UserVO> regist(UserVO user) {
        String regex = "\\d{11}";
        if (user.getUsername().matches(regex)) {
            return ActResult.fail("用户名不能为11位数字！");
        }

        if (user.getUsername().contains("@")) {
            return ActResult.fail("用户名不能包含@！");
        }

        if (StringUtils.hasText(user.getPhoneNumber()) && StringUtils.hasText(user.getEmail())) {
            return ActResult.fail("手机号与邮箱只能选择其中一个！");
        }

        if (StringUtils.hasText(user.getPhoneNumber())) {
            Integer codeCache = (Integer) SysCache.get(CommonConst.FORGET_PASSWORD + user.getPhoneNumber() + "_1");
            if (codeCache == null || codeCache != Integer.parseInt(user.getCode())) {
                return ActResult.fail("验证码错误！");
            }
            SysCache.remove(CommonConst.FORGET_PASSWORD + user.getPhoneNumber() + "_1");
        } else if (StringUtils.hasText(user.getEmail())) {
            Integer codeCache = (Integer) SysCache.get(CommonConst.FORGET_PASSWORD + user.getEmail() + "_2");
            if (codeCache == null || codeCache != Integer.parseInt(user.getCode())) {
                return ActResult.fail("验证码错误！");
            }
            SysCache.remove(CommonConst.FORGET_PASSWORD + user.getEmail() + "_2");
        } else {
            return ActResult.fail("请输入邮箱或手机号！");
        }


        user.setPassword(new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(user.getPassword())));

        Integer count = lambdaQuery().eq(UserPO::getUsername, user.getUsername()).count().intValue();
        if (count != 0) {
            return ActResult.fail("用户名重复！");
        }
        if (StringUtils.hasText(user.getPhoneNumber())) {
            Integer phoneNumberCount = lambdaQuery().eq(UserPO::getPhoneNumber, user.getPhoneNumber()).count().intValue();
            if (phoneNumberCount != 0) {
                return ActResult.fail("手机号重复！");
            }
        } else if (StringUtils.hasText(user.getEmail())) {
            Integer emailCount = lambdaQuery().eq(UserPO::getEmail, user.getEmail()).count().intValue();
            if (emailCount != 0) {
                return ActResult.fail("邮箱重复！");
            }
        }

        UserPO u = new UserPO();
        u.setUsername(user.getUsername());
        u.setPhoneNumber(user.getPhoneNumber());
        u.setEmail(user.getEmail());
        u.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        u.setAvatar(PoetryUtil.getRandomAvatar(null));
        save(u);

        UserPO one = lambdaQuery().eq(UserPO::getId, u.getId()).one();

        String userToken = CommonConst.USER_ACCESS_TOKEN + UUID.randomUUID().toString().replaceAll("-", "");
        SysCache.put(userToken, one, CommonConst.TOKEN_EXPIRE);
        SysCache.put(CommonConst.USER_TOKEN + one.getId(), userToken, CommonConst.TOKEN_EXPIRE);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        userVO.setAccessToken(userToken);

        WeiYanPO weiYanPO = new WeiYanPO();
        weiYanPO.setUserId(one.getId());
        weiYanPO.setContent("到此一游");
        weiYanPO.setType(CommonConst.WEIYAN_TYPE_FRIEND);
        weiYanPO.setIsPublic(Boolean.TRUE);
        weiYanPow.save(weiYanPO);

        ChatGroupUserPO chatGroupUserPO = new ChatGroupUserPO();
        chatGroupUserPO.setGroupId(ImConfigConst.DEFAULT_GROUP_ID);
        chatGroupUserPO.setUserId(one.getId());
        chatGroupUserPO.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        imChatGroupUserMapper.insert(chatGroupUserPO);

        ChatUserFriendPO imChatUser = new ChatUserFriendPO();
        imChatUser.setUserId(one.getId());
        imChatUser.setFriendId(PoetryUtil.getAdminUser().getId());
        imChatUser.setRemark("站长");
        imChatUser.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        imChatUserFriendMapper.insert(imChatUser);

        ChatUserFriendPO imChatFriend = new ChatUserFriendPO();
        imChatFriend.setUserId(PoetryUtil.getAdminUser().getId());
        imChatFriend.setFriendId(one.getId());
        imChatFriend.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        imChatUserFriendMapper.insert(imChatFriend);

        return ActResult.success(userVO);
    }

    @Override
    public ActResult<UserVO> updateUserInfo(UserVO user) {
        if (StringUtils.hasText(user.getUsername())) {
            String regex = "\\d{11}";
            if (user.getUsername().matches(regex)) {
                return ActResult.fail("用户名不能为11位数字！");
            }

            if (user.getUsername().contains("@")) {
                return ActResult.fail("用户名不能包含@！");
            }

            Integer count = lambdaQuery().eq(UserPO::getUsername, user.getUsername()).ne(UserPO::getId, PoetryUtil.getUserId()).count().intValue();
            if (count != 0) {
                return ActResult.fail("用户名重复！");
            }
        }
        UserPO u = new UserPO();
        u.setId(PoetryUtil.getUserId());
        u.setUsername(user.getUsername());
        u.setAvatar(user.getAvatar());
        u.setGender(user.getGender());
        u.setIntroduction(user.getIntroduction());
        updateById(u);
        UserPO one = lambdaQuery().eq(UserPO::getId, u.getId()).one();
        SysCache.put(PoetryUtil.getToken(), one, CommonConst.TOKEN_EXPIRE);
        SysCache.put(CommonConst.USER_TOKEN + one.getId(), PoetryUtil.getToken(), CommonConst.TOKEN_EXPIRE);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        userVO.setAccessToken(PoetryUtil.getToken());
        return ActResult.success(userVO);
    }

    @Override
    public ActResult getCode(Integer flag) {
        UserPO userPO = PoetryUtil.getCurrentUser();
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            if (!StringUtils.hasText(userPO.getPhoneNumber())) {
                return ActResult.fail("请先绑定手机号！");
            }

            log.info(userPO.getId() + "---" + userPO.getUsername() + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            if (!StringUtils.hasText(userPO.getEmail())) {
                return ActResult.fail("请先绑定邮箱！");
            }

            log.info(userPO.getId() + "---" + userPO.getUsername() + "---" + "邮箱验证码---" + i);

            List<String> mail = new ArrayList<>();
            mail.add(userPO.getEmail());
            String text = getCodeMail(i);
            WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);

            AtomicInteger count = (AtomicInteger) SysCache.get(CommonConst.CODE_MAIL + mail.get(0));
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfoPO == null ? "POETIZE" : webInfoPO.getWebName()) + "的回执！", text);
                if (count == null) {
                    SysCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return ActResult.fail("验证码发送次数过多，请明天再试！");
            }
        }
        SysCache.put(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + flag, Integer.valueOf(i), 300);
        return ActResult.success();
    }

    @Override
    public ActResult getCodeForBind(String place, Integer flag) {
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            log.info(place + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            log.info(place + "---" + "邮箱验证码---" + i);
            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(i);
            WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);

            AtomicInteger count = (AtomicInteger) SysCache.get(CommonConst.CODE_MAIL + mail.get(0));
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfoPO == null ? "POETIZE" : webInfoPO.getWebName()) + "的回执！", text);
                if (count == null) {
                    SysCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return ActResult.fail("验证码发送次数过多，请明天再试！");
            }
        }
        SysCache.put(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag, Integer.valueOf(i), 300);
        return ActResult.success();
    }

    @Override
    public ActResult<UserVO> updateSecretInfo(String place, Integer flag, String code, String password) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

        UserPO userPO = PoetryUtil.getCurrentUser();
        if ((flag == 1 || flag == 2) && !DigestUtils.md5DigestAsHex(password.getBytes()).equals(userPO.getPassword())) {
            return ActResult.fail("密码错误！");
        }
        if ((flag == 1 || flag == 2) && !StringUtils.hasText(code)) {
            return ActResult.fail("请输入验证码！");
        }
        UserPO updateUserPO = new UserPO();
        updateUserPO.setId(userPO.getId());
        if (flag == 1) {
            Integer count = lambdaQuery().eq(UserPO::getPhoneNumber, place).count().intValue();
            if (count != 0) {
                return ActResult.fail("手机号重复！");
            }
            Integer codeCache = (Integer) SysCache.get(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                SysCache.remove(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);

                updateUserPO.setPhoneNumber(place);
            } else {
                return ActResult.fail("验证码错误！");
            }

        } else if (flag == 2) {
            Integer count = lambdaQuery().eq(UserPO::getEmail, place).count().intValue();
            if (count != 0) {
                return ActResult.fail("邮箱重复！");
            }
            Integer codeCache = (Integer) SysCache.get(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                SysCache.remove(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);

                updateUserPO.setEmail(place);
            } else {
                return ActResult.fail("验证码错误！");
            }
        } else if (flag == 3) {
            if (DigestUtils.md5DigestAsHex(place.getBytes()).equals(userPO.getPassword())) {
                updateUserPO.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
            } else {
                return ActResult.fail("密码错误！");
            }
        }
        updateById(updateUserPO);

        UserPO one = lambdaQuery().eq(UserPO::getId, userPO.getId()).one();
        SysCache.put(PoetryUtil.getToken(), one, CommonConst.TOKEN_EXPIRE);
        SysCache.put(CommonConst.USER_TOKEN + one.getId(), PoetryUtil.getToken(), CommonConst.TOKEN_EXPIRE);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        return ActResult.success(userVO);
    }

    @Override
    public ActResult getCodeForForgetPassword(String place, Integer flag) {
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            log.info(place + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            log.info(place + "---" + "邮箱验证码---" + i);

            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(i);
            WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);

            AtomicInteger count = (AtomicInteger) SysCache.get(CommonConst.CODE_MAIL + mail.get(0));
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfoPO == null ? "POETIZE" : webInfoPO.getWebName()) + "的回执！", text);
                if (count == null) {
                    SysCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return ActResult.fail("验证码发送次数过多，请明天再试！");
            }
        }
        SysCache.put(CommonConst.FORGET_PASSWORD + place + "_" + flag, Integer.valueOf(i), 300);
        return ActResult.success();
    }

    @Override
    public ActResult updateForForgetPassword(String place, Integer flag, String code, String password) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

        Integer codeCache = (Integer) SysCache.get(CommonConst.FORGET_PASSWORD + place + "_" + flag);
        if (codeCache == null || codeCache != Integer.parseInt(code)) {
            return ActResult.fail("验证码错误！");
        }

        SysCache.remove(CommonConst.FORGET_PASSWORD + place + "_" + flag);

        if (flag == 1) {
            UserPO userPO = lambdaQuery().eq(UserPO::getPhoneNumber, place).one();
            if (userPO == null) {
                return ActResult.fail("该手机号未绑定账号！");
            }

            if (!userPO.getUserStatus()) {
                return ActResult.fail("账号被冻结！");
            }

            lambdaUpdate().eq(UserPO::getPhoneNumber, place).set(UserPO::getPassword, DigestUtils.md5DigestAsHex(password.getBytes())).update();
            SysCache.remove(CommonConst.USER_CACHE + userPO.getId().toString());
        } else if (flag == 2) {
            UserPO userPO = lambdaQuery().eq(UserPO::getEmail, place).one();
            if (userPO == null) {
                return ActResult.fail("该邮箱未绑定账号！");
            }

            if (!userPO.getUserStatus()) {
                return ActResult.fail("账号被冻结！");
            }

            lambdaUpdate().eq(UserPO::getEmail, place).set(UserPO::getPassword, DigestUtils.md5DigestAsHex(password.getBytes())).update();
            SysCache.remove(CommonConst.USER_CACHE + userPO.getId().toString());
        }

        return ActResult.success();
    }

    @Override
    public ActResult<Page> listUser(BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<UserPO> lambdaQuery = lambdaQuery();

        if (baseRequestVO.getUserStatus() != null) {
            lambdaQuery.eq(UserPO::getUserStatus, baseRequestVO.getUserStatus());
        }

        if (baseRequestVO.getUserType() != null) {
            lambdaQuery.eq(UserPO::getUserType, baseRequestVO.getUserType());
        }

        if (StringUtils.hasText(baseRequestVO.getSearchKey())) {
            lambdaQuery.and(lq -> lq.like(UserPO::getUsername, baseRequestVO.getSearchKey())
                    .or()
                    .like(UserPO::getPhoneNumber, baseRequestVO.getSearchKey())
                    .or()
                    .like(UserPO::getEmail, baseRequestVO.getSearchKey()));
        }

        lambdaQuery.orderByDesc(UserPO::getCreateTime).page(baseRequestVO);

        List<UserPO> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            records.forEach(u -> {
                u.setPassword(null);
                u.setOpenId(null);
            });
        }
        return ActResult.success(baseRequestVO);
    }

    @Override
    public ActResult<List<UserVO>> getUserByUsername(String username) {
        List<UserPO> userPOS = lambdaQuery().select(UserPO::getId, UserPO::getUsername, UserPO::getAvatar, UserPO::getGender, UserPO::getIntroduction).like(UserPO::getUsername, username).last("limit 5").list();
        List<UserVO> userVOS = userPOS.stream().map(u -> {
            UserVO userVO = new UserVO();
            userVO.setId(u.getId());
            userVO.setUsername(u.getUsername());
            userVO.setAvatar(u.getAvatar());
            userVO.setIntroduction(u.getIntroduction());
            userVO.setGender(u.getGender());
            return userVO;
        }).collect(Collectors.toList());
        return ActResult.success(userVOS);
    }

    @Override
    public ActResult<UserVO> token(String userToken) {
        userToken = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(userToken));

        if (!StringUtils.hasText(userToken)) {
            throw new SysRuntimeException("未登陆，请登陆后再进行操作！");
        }

        UserPO userPO = (UserPO) SysCache.get(userToken);

        if (userPO == null) {
            throw new SysRuntimeException("登录已过期，请重新登陆！");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userPO, userVO);
        userVO.setPassword(null);

        userVO.setAccessToken(userToken);

        return ActResult.success(userVO);
    }

    @Override
    public ActResult<UserVO> subscribe(Integer labelId, Boolean flag) {
        UserVO userVO = null;
        UserPO one = lambdaQuery().eq(UserPO::getId, PoetryUtil.getUserId()).one();
        List<Integer> sub = JSON.parseArray(one.getSubscribe(), Integer.class);
        if (sub == null) sub = new ArrayList<>();
        if (flag) {
            if (!sub.contains(labelId)) {
                sub.add(labelId);
                UserPO userPO = new UserPO();
                userPO.setId(one.getId());
                userPO.setSubscribe(JSON.toJSONString(sub));
                updateById(userPO);

                userVO = new UserVO();
                BeanUtils.copyProperties(one, userVO);
                userVO.setPassword(null);
                userVO.setSubscribe(userPO.getSubscribe());
                userVO.setAccessToken(PoetryUtil.getToken());
            }
        } else {
            if (sub.contains(labelId)) {
                sub.remove(labelId);
                UserPO userPO = new UserPO();
                userPO.setId(one.getId());
                userPO.setSubscribe(JSON.toJSONString(sub));
                updateById(userPO);

                userVO = new UserVO();
                BeanUtils.copyProperties(one, userVO);
                userVO.setPassword(null);
                userVO.setSubscribe(userPO.getSubscribe());
                userVO.setAccessToken(PoetryUtil.getToken());
            }
        }
        return ActResult.success(userVO);
    }

    private String getCodeMail(int i) {
        WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);
        String webName = (webInfoPO == null ? "POETIZE-NEXT" : webInfoPO.getWebName());
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, PoetryUtil.getAdminUser().getUsername()),
                PoetryUtil.getAdminUser().getUsername(),
                String.format(codeFormat, i),
                "",
                webName);
    }
}
