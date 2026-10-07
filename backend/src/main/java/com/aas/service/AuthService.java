package com.aas.service;

import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.LoginDTO;
import com.aas.entity.BaseSemester;
import com.aas.entity.StuStudent;
import com.aas.entity.SysMenu;
import com.aas.entity.SysUser;
import com.aas.entity.TeaTeacher;
import com.aas.mapper.BaseSemesterMapper;
import com.aas.mapper.CourseOfferingMapper;
import com.aas.mapper.StuStudentMapper;
import com.aas.mapper.SysMenuMapper;
import com.aas.mapper.SysUserMapper;
import com.aas.mapper.TeaTeacherMapper;
import com.aas.security.JwtUtils;
import com.aas.security.LoginUser;
import com.aas.util.MenuTreeUtil;
import com.aas.vo.LoginVO;
import com.aas.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final StuStudentMapper studentMapper;
    private final TeaTeacherMapper teacherMapper;
    private final BaseSemesterMapper semesterMapper;
    private final CourseOfferingMapper offeringMapper;
    private final com.aas.security.UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 账号密码登录
     */
    public LoginVO login(LoginDTO dto, String ip) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())
                .last("LIMIT 1"));
        if (user == null) {
            throw new BizException(ResultCode.LOGIN_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.LOGIN_ERROR);
        }

        // 更新登录信息
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, ip));

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtUtils.getExpireSeconds());
        vo.setUserInfo(buildUserInfo(userDetailsService.loadUserById(user.getId())));
        return vo;
    }

    /**
     * 获取当前登录用户信息(含菜单)
     */
    public UserInfoVO getInfo(LoginUser loginUser) {
        return buildUserInfo(loginUser);
    }

    /**
     * 获取当前用户的菜单树
     */
    public List<SysMenu> getMenuTree(LoginUser loginUser) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(loginUser.getUserId());
        return MenuTreeUtil.build(menus, 0L);
    }

    /**
     * 修改自己的密码
     */
    public void changePassword(LoginUser loginUser, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword)));
    }

    /**
     * 组装登录用户信息
     */
    private UserInfoVO buildUserInfo(LoginUser loginUser) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(loginUser, vo);
        vo.setManageClassIds(loginUser.getManageClassIds());
        vo.setMenus(getMenuTree(loginUser));

        if (loginUser.isStudent() && loginUser.getStudentId() != null) {
            StuStudent student = studentMapper.selectStudentDetail(loginUser.getStudentId());
            if (student != null) {
                vo.setStudentNo(student.getStudentNo());
                vo.setClassName(student.getClassName());
                vo.setDeptName(student.getDeptName());
            }
        }
        if (loginUser.getTeacherId() != null) {
            TeaTeacher teacher = teacherMapper.selectTeacherDetail(loginUser.getTeacherId());
            if (teacher != null) {
                vo.setTeacherNo(teacher.getTeacherNo());
                vo.setDeptName(teacher.getDeptName());
                vo.setManageClassNames(teacherMapper.selectManageClassNames(teacher.getId()));
                vo.setTeachingCount(offeringMapper.selectCount(new LambdaQueryWrapper<com.aas.entity.CourseOffering>()
                        .eq(com.aas.entity.CourseOffering::getTeacherId, teacher.getId())).intValue());
            }
        }
        return vo;
    }

    /**
     * 获取当前学期
     */
    public BaseSemester getCurrentSemester() {
        BaseSemester semester = semesterMapper.selectOne(new LambdaQueryWrapper<BaseSemester>()
                .eq(BaseSemester::getIsCurrent, 1)
                .last("LIMIT 1"));
        if (semester == null) {
            semester = semesterMapper.selectOne(new LambdaQueryWrapper<BaseSemester>()
                    .orderByDesc(BaseSemester::getId)
                    .last("LIMIT 1"));
        }
        return semester;
    }
}
