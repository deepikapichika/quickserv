package com.quickserv.quickserv.config;

import com.quickserv.quickserv.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        User loggedInUser = session == null ? null : (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        if (path.startsWith("/admin/") && !"ADMIN".equals(loggedInUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return false;
        }

        if (requiresProviderRole(path) && !"PROVIDER".equals(loggedInUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.equals("/hello")
                || path.equals("/error")
                || path.equals("/categories/all")
                || path.equals("/category/all")
                || path.equals("/locations/all")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.startsWith("/services/search")
                || path.startsWith("/services/quick-search")
                || path.startsWith("/provider/byCategory/")
                || path.startsWith("/provider/byLocation/");
    }

    private boolean requiresProviderRole(String path) {
        return path.equals("/provider/dashboard")
                || path.equals("/provider/bookings")
                || path.equals("/provider/register")
                || path.equals("/provider/profile/update")
                || path.startsWith("/provider/services")
                || path.startsWith("/provider/booking/");
    }
}
