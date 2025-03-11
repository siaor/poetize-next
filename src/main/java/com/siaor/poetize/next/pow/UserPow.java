package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.UserVO;

import java.util.List;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
public interface UserPow extends IService<UserPO> {

    /**
     * 用户名、邮箱、手机号/密码登录
     *
     * @param account
     * @param password
     * @return
     */
    ActResult<UserVO> login(String account, String password, Boolean isAdmin);

    ActResult exit();

    ActResult<UserVO> regist(UserVO user);

    ActResult<UserVO> updateUserInfo(UserVO user);

    ActResult getCode(Integer flag);

    ActResult getCodeForBind(String place, Integer flag);

    ActResult<UserVO> updateSecretInfo(String place, Integer flag, String code, String password);

    ActResult getCodeForForgetPassword(String place, Integer flag);

    ActResult updateForForgetPassword(String place, Integer flag, String code, String password);

    ActResult<Page> listUser(BaseRequestVO baseRequestVO);

    ActResult<List<UserVO>> getUserByUsername(String username);

    ActResult<UserVO> token(String userToken);

    ActResult<UserVO> subscribe(Integer labelId, Boolean flag);
}
