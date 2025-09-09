package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.UserStatusEnum;
import com.noah.superagent.dao.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 根据手机号查询用户
     */
    default UserEntity selectByPhone(String phone) {
        return selectOneByQuery(QueryWrapper.create()
                .where(UserEntity::getPhone).eq(phone));
    }

    /**
     * 分页查询用户（带搜索）
     */
    default Page<UserEntity> selectUserPage(Page<UserEntity> page, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(UserEntity::getUsername).like(keyword, keyword != null)
                .or(UserEntity::getPhone).like(keyword, keyword != null)
                .orderBy(UserEntity::getCreateTime).desc();
        
        return paginate(page, query);
    }

    /**
     * 根据用户状态查询用户列表
     */
    default List<UserEntity> selectByStatus(Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserEntity::getStatus).eq(status)
                .orderBy(UserEntity::getCreateTime).desc());
    }

    /**
     * 根据用户名模糊查询用户列表
     */
    default List<UserEntity> selectByUsername(String username) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserEntity::getUsername).like(username)
                .orderBy(UserEntity::getCreateTime).desc());
    }

    /**
     * 更新用户状态
     */
    default int updateStatusByPhone(String phone, UserStatusEnum status) {
        // 创建仅包含状态字段的更新对象
        UserEntity updateUserEntity = new UserEntity();
        updateUserEntity.setStatus(status);
        return updateByQuery(updateUserEntity,
                QueryWrapper.create().where(UserEntity::getPhone).eq(phone));
    }
}
