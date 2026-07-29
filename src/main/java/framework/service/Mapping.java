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
    private String httpMethod; // "GET", "POST", ou "ALL"
    
    public Mapping() {
    }

    public Mapping(String className, Method method, String url, String httpMethod) {
        this.className = className;
        this.method = method;
        this.url = url;
        this.httpMethod = httpMethod;
    }
    
    public boolean matchesUrl(String requestUrl) {
        String pattern = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
        pattern = "^" + pattern + "$";
        return requestUrl.matches(pattern);
    }
    
    public Map<String, String> extractUrlParams(String requestUrl) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        
        Pattern namePattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher nameMatcher = namePattern.matcher(url);
        
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
    
    public boolean hasDynamicParams() {
        return url.contains("{") && url.contains("}");
    }
    
    // Getters et Setters
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
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}