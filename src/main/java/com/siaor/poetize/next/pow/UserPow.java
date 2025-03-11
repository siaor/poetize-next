package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.repo.po.UserPO;
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
    PoetryResult<UserVO> login(String account, String password, Boolean isAdmin);

    PoetryResult exit();

    PoetryResult<UserVO> regist(UserVO user);

    PoetryResult<UserVO> updateUserInfo(UserVO user);

    PoetryResult getCode(Integer flag);

    PoetryResult getCodeForBind(String place, Integer flag);

    PoetryResult<UserVO> updateSecretInfo(String place, Integer flag, String code, String password);

    PoetryResult getCodeForForgetPassword(String place, Integer flag);

    PoetryResult updateForForgetPassword(String place, Integer flag, String code, String password);

    PoetryResult<Page> listUser(BaseRequestVO baseRequestVO);

    PoetryResult<List<UserVO>> getUserByUsername(String username);

    PoetryResult<UserVO> token(String userToken);

    PoetryResult<UserVO> subscribe(Integer labelId, Boolean flag);
}
