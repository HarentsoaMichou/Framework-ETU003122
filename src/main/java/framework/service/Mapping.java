package framework.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mapping {
    
    private String className;
    private Method method;
    private String url;
    
    public Mapping() {
    }

    public Mapping(String className, Method method, String url) {
        this.className = className;
        this.method = method;
        this.url = url;
    }
    
    /**
     * Vérifie si l'URL correspond au pattern avec paramètres
     * Exemple : /api/users/{id} correspond à /api/users/123
     */
    public boolean matchesUrl(String requestUrl) {
        String pattern = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
        pattern = "^" + pattern + "$";
        return requestUrl.matches(pattern);
    }
    
    /**
     * Extrait les valeurs des paramètres depuis l'URL
     * Exemple : /api/users/{id}/posts/{postId} + /api/users/123/posts/456
     * → Map("id" -> "123", "postId" -> "456")
     */
    public Map<String, String> extractUrlParams(String requestUrl) {
        Map<String, String> params = new HashMap<>();
        
        // Extraire les noms des paramètres depuis le pattern
        Pattern namePattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher nameMatcher = namePattern.matcher(url);
        
        // Créer un pattern regex pour matcher l'URL
        String pattern = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
        Pattern valuePattern = Pattern.compile(pattern);
        Matcher valueMatcher = valuePattern.matcher(requestUrl);
        
        if (valueMatcher.matches()) {
            int groupIndex = 1;
            nameMatcher.reset();
            
            while (nameMatcher.find()) {
                String paramName = nameMatcher.group(1);
                String paramValue = valueMatcher.group(groupIndex);
                params.put(paramName, paramValue);
                groupIndex++;
            }
        }
        
        return params;
    }
    
    /**
     * Vérifie si l'URL contient des paramètres dynamiques
     */
    public boolean hasDynamicParams() {
        return url.contains("{") && url.contains("}");
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public void setMethod(Method method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}