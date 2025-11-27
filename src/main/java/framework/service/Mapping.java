package framework.service;

import java.lang.reflect.Method;

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