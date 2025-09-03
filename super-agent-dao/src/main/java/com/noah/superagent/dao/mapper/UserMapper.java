package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户
     */
    default User selectByPhone(String phone) {
        return selectOneByQuery(QueryWrapper.create()
                .where(User::getPhone).eq(phone));
    }

    /**
     * 分页查询用户（带搜索）
     */
    default Page<User> selectUserPage(Page<User> page, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(User::getNickname).like(keyword, keyword != null)
                .or(User::getPhone).like(keyword, keyword != null)
                .orderBy(User::getCreateTime).desc();
        
        return paginate(page, query);
    }

    /**
     * 根据用户状态查询用户列表
     */
    default List<User> selectByStatus(Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(User::getStatus).eq(status)
                .orderBy(User::getCreateTime).desc());
    }

    /**
     * 根据昵称模糊查询用户列表
     */
    default List<User> selectByNickname(String nickname) {
        return selectListByQuery(QueryWrapper.create()
                .where(User::getNickname).like(nickname)
                .orderBy(User::getCreateTime).desc());
    }

    /**
     * 更新用户状态
     */
    default int updateStatusByPhone(String phone, Integer status) {
        // 创建仅包含状态字段的更新对象
        User updateUser = new User();
        updateUser.setStatus(status);
        return updateByQuery(updateUser, 
                QueryWrapper.create().where(User::getPhone).eq(phone));
    }
}
