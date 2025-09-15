package com.noah.superagent.config;

import com.noah.superagent.util.SSOManager;
import com.noah.superagent.util.UserContext;
import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* SSO拦截器，用于拦截未登录用户并跳转到SSO登录页面
*
* @author 任相鹏
* @since 1.0.0
*/
@Slf4j
@Component
@RequiredArgsConstructor
public class SSOInterceptor implements HandlerInterceptor {

   private final SSOManager ssoManager;
   private final UserCreditService userCreditService;

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       String requestURI = request.getRequestURI();
       String queryString = request.getQueryString();
       String fullURL = requestURI + (queryString != null ? "?" + queryString : "");

       log.debug("SSO拦截器处理请求: {}", fullURL);

       // 检查用户是否已登录
       if (!isUserLoggedIn()) {
           log.info("用户未登录，重定向到SSO登录页面: {}", fullURL);

           // 构建当前页面URL作为登录成功后的回调地址
           String currentUrl = request.getRequestURL().toString();
           if (queryString != null) {
               currentUrl += "?" + queryString;
           }

           // 重定向到SSO登录页面
           String loginUrl = ssoManager.getLoginUrl(currentUrl);
           response.sendRedirect(loginUrl);
           return false;
       }

       return true;
   }

   /**
    * 检查用户是否已登录
    * 如果已登录，将用户信息设置到上下文中
    */
   private boolean isUserLoggedIn() {
       try {
           // 从SSO获取用户信息
           SSOUserInfo user = ssoManager.getCurrentSSOUser();
           if (user != null) {
               // 将用户信息设置到上下文中，供后续业务逻辑使用
               UserContext.setCurrentUser(user);
               log.debug("用户已登录: userId={}, userName={}", user.getUserId(), user.getUserName());
               
               // 自动检查并创建积分账户
               autoInitUserCreditIfNeeded(user);
               
               return true;
           }
       } catch (Exception e) {
           log.error("检查用户登录状态时发生异常: {}", e.getMessage(), e);
       }

       return false;
   }

   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       // 请求结束后清理用户上下文
       UserContext.clearCurrentUser();
   }
   
   /**
    * 自动检查并创建用户积分账户
    * 如果用户没有积分账户，则自动创建并分配免费套餐
    */
   private void autoInitUserCreditIfNeeded(SSOUserInfo user) {
       if (user == null || user.getUserId() == null) {
           return;
       }
       
       try {
           Long userId = Long.valueOf(user.getUserId());
           
           // 检查用户是否已有积分账户
           if (!userCreditService.hasUserCredit(userId)) {
               log.info("用户首次登录，自动创建积分账户 - userId: {}, username: {}", 
                       userId, user.getUserName());
               
               // 创建积分账户并分配免费套餐
               userCreditService.initFreePlanForUser(userId);
               
               log.info("用户积分账户创建完成 - userId: {}", userId);
           }
           
       } catch (Exception e) {
           log.error("自动创建用户积分账户失败 - userId: {}, username: {}, 错误: {}", 
                   user.getUserId(), user.getUserName(), e.getMessage(), e);
           // 不抛出异常，避免影响用户正常登录
       }
   }
}
