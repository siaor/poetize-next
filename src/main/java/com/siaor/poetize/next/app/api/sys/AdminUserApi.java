package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.enums.CodeMsg;
import com.siaor.poetize.next.res.enums.SysEnum;
import com.siaor.poetize.next.res.websocket.TioUtil;
import com.siaor.poetize.next.res.websocket.TioWebsocketStarter;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tio.core.Tio;

/**
 * <p>
 * 后台用户 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminUserApi {

    @Autowired
    private UserPow userPow;

    /**
     * 查询用户
     */
    @PostMapping("/user/list")
    @LoginCheck(0)
    public PoetryResult<Page> listUser(@RequestBody BaseRequestVO baseRequestVO) {
        return userPow.listUser(baseRequestVO);
    }

    /**
     * 修改用户状态
     * <p>
     * flag = true：解禁
     * flag = false：封禁
     */
    @GetMapping("/user/changeUserStatus")
    @LoginCheck(0)
    public PoetryResult changeUserStatus(@RequestParam("userId") Integer userId, @RequestParam("flag") Boolean flag) {
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            return PoetryResult.fail("站长状态不能修改！");
        }

        LambdaUpdateChainWrapper<UserPO> updateChainWrapper = userPow.lambdaUpdate().eq(UserPO::getId, userId);
        if (flag) {
            updateChainWrapper.eq(UserPO::getUserStatus, SysEnum.STATUS_DISABLE.getCode()).set(UserPO::getUserStatus, SysEnum.STATUS_ENABLE.getCode()).update();
        } else {
            updateChainWrapper.eq(UserPO::getUserStatus, SysEnum.STATUS_ENABLE.getCode()).set(UserPO::getUserStatus, SysEnum.STATUS_DISABLE.getCode()).update();
        }
        logout(userId);
        return PoetryResult.success();
    }

    /**
     * 修改用户赞赏
     */
    @GetMapping("/user/changeUserAdmire")
    @LoginCheck(0)
    public PoetryResult changeUserAdmire(@RequestParam("userId") Integer userId, @RequestParam("admire") String admire) {
        userPow.lambdaUpdate()
                .eq(UserPO::getId, userId)
                .set(UserPO::getAdmire, admire)
                .update();
        PoetryCache.remove(CommonConst.ADMIRE);
        return PoetryResult.success();
    }

    /**
     * 修改用户类型
     */
    @GetMapping("/user/changeUserType")
    @LoginCheck(0)
    public PoetryResult changeUserType(@RequestParam("userId") Integer userId, @RequestParam("userType") Integer userType) {
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            return PoetryResult.fail("站长类型不能修改！");
        }

        if (userType != 0 && userType != 1 && userType != 2) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        userPow.lambdaUpdate().eq(UserPO::getId, userId).set(UserPO::getUserType, userType).update();

        logout(userId);
        return PoetryResult.success();
    }

    private void logout(Integer userId) {
        if (PoetryCache.get(CommonConst.ADMIN_TOKEN + userId) != null) {
            String token = (String) PoetryCache.get(CommonConst.ADMIN_TOKEN + userId);
            PoetryCache.remove(CommonConst.ADMIN_TOKEN + userId);
            PoetryCache.remove(token);
        }

        if (PoetryCache.get(CommonConst.USER_TOKEN + userId) != null) {
            String token = (String) PoetryCache.get(CommonConst.USER_TOKEN + userId);
            PoetryCache.remove(CommonConst.USER_TOKEN + userId);
            PoetryCache.remove(token);
        }
        TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
        if (tioWebsocketStarter != null) {
            Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
        }

    }
}
