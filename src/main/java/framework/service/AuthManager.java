// gère la logique de connexion, déconnexion et de validation des autorisations. Les clés de session et l'URL de connexion peuvent être lues depuis le web.xml ou utiliser des valeurs par défaut.

package framework.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AuthManager {
    
    private final String userSessionKey;
    private final String roleSessionKey;
    private final String loginPageUrl;
    
    public AuthManager(String userSessionKey, String roleSessionKey, String loginPageUrl) {
        this.userSessionKey = userSessionKey;
        this.roleSessionKey = roleSessionKey;
        this.loginPageUrl = loginPageUrl;
    }
    
    public boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(userSessionKey) != null;
    }
    
    @SuppressWarnings("unchecked")
    public boolean hasRequiredRole(HttpServletRequest request, String[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        List<String> userRoles = (List<String>) session.getAttribute(roleSessionKey);
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        
        return userRoles.stream()
                .anyMatch(userRole -> Arrays.asList(requiredRoles).contains(userRole));
    }
    
    public void redirectToLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String contextPath = request.getContextPath();
        response.sendRedirect(contextPath + loginPageUrl);
    }
    
    public void loginUser(HttpServletRequest request, String username, List<String> roles) {
        HttpSession session = request.getSession(true);
        session.setAttribute(userSessionKey, username);
        session.setAttribute(roleSessionKey, roles);
        session.setMaxInactiveInterval(30 * 60); // 30 minutes de validité
    }
    
    public void logoutUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
