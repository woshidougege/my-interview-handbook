package com.noah.superagent.common.constants;

/**
 * 业务常量定义
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
public final class BusinessConstants {

    private BusinessConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 套餐相关常量
     */
    public static final class Plans {
        /** 积分套餐ID */
        public static final Long CREDIT_PACKAGE_PLAN_ID = 100L;
        
        private Plans() {}
    }

    /**
     * 订单相关常量
     */
    public static final class Order {
        /** 订单号前缀 */
        public static final String ORDER_PREFIX = "ORDER_";
        /** 退款单号前缀 */
        public static final String REFUND_PREFIX = "REFUND_";
        /** 测试退款单号前缀 */
        public static final String TEST_REFUND_PREFIX = "TEST_REFUND_";
        /** 微信交易号前缀 */
        public static final String WX_TRANSACTION_PREFIX = "wx_transaction_";
        
        private Order() {}
    }

    /**
     * 任务类型常量
     */
    public static final class TaskType {
        /** 普通任务 */
        public static final Integer NORMAL = 0;
        /** 特殊任务 */
        public static final Integer SPECIAL = 1;
        
        private TaskType() {}
    }
}
