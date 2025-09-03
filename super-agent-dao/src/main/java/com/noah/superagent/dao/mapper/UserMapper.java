package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.User;
import org.apache.ibatis.annotations.Mapper;

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
}
