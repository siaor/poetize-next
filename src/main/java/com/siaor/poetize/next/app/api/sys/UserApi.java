package com.siaor.poetize.next.app.api.sys;


import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.oper.aop.SaveCheck;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@RestController
@RequestMapping("/user")
public class UserApi {

    @Autowired
    private UserPow userPow;


    /**
     * 用户名/密码注册
     */
    @PostMapping("/regist")
    public ActResult<UserVO> regist(@Validated @RequestBody UserVO user) {
        return userPow.regist(user);
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     */
    @PostMapping("/login")
    public ActResult<UserVO> login(@RequestParam("account") String account,
                                   @RequestParam("password") String password,
                                   @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {
        return userPow.login(account, password, isAdmin);
    }


    /**
     * Token登录
     */
    @PostMapping("/token")
    public ActResult<UserVO> login(@RequestParam("userToken") String userToken) {
        return userPow.token(userToken);
    }


    /**
     * 退出
     */
    @GetMapping("/logout")
    @LoginCheck
    public ActResult exit() {
        return userPow.exit();
    }


    /**
     * 更新用户信息
     */
    @PostMapping("/updateUserInfo")
    @LoginCheck
    public ActResult<UserVO> updateUserInfo(@RequestBody UserVO user) {
        SysCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userPow.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCode")
    @LoginCheck
    @SaveCheck
    public ActResult getCode(@RequestParam("flag") Integer flag) {
        return userPow.getCode(flag);
    }

    /**
     * 绑定手机号或者邮箱
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForBind")
    @LoginCheck
    @SaveCheck
    public ActResult getCodeForBind(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userPow.getCodeForBind(place, flag);
    }

    /**
     * 更新邮箱、手机号、密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     */
    @PostMapping("/updateSecretInfo")
    @LoginCheck
    public ActResult<UserVO> updateSecretInfo(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam(value = "code", required = false) String code, @RequestParam("password") String password) {
        SysCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userPow.updateSecretInfo(place, flag, code, password);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForForgetPassword")
    @SaveCheck
    public ActResult getCodeForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userPow.getCodeForForgetPassword(place, flag);
    }

    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @PostMapping("/updateForForgetPassword")
    public ActResult updateForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam("code") String code, @RequestParam("password") String password) {
        return userPow.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     */
    @GetMapping("/getUserByUsername")
    @LoginCheck
    public ActResult<List<UserVO>> getUserByUsername(@RequestParam("username") String username) {
        return userPow.getUserByUsername(username);
    }

    /**
     * 订阅/取消订阅专栏（标签）
     * <p>
     * flag = true：订阅
     * flag = false：取消订阅
     */
    @GetMapping("/subscribe")
    @LoginCheck
    public ActResult<UserVO> subscribe(@RequestParam("labelId") Integer labelId, @RequestParam("flag") Boolean flag) {
        SysCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userPow.subscribe(labelId, flag);
    }
}

