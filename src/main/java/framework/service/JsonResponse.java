package framework.service;

import java.util.*;

public class JsonResponse {
    private String status;  // "success" ou "error"
    private int code;       // 200, 201, 400, 500, etc.
    private Object data;    // Les données
    private Integer count;  // Pour les listes (optionnel)
    
    // Constructeur success
    public JsonResponse(int code, Object data) {
        this.status = "success";
        this.code = code;
        this.data = data;
        
        // Si c'est une collection, compter les éléments
        if (data instanceof Collection) {
            this.count = ((Collection<?>) data).size();
        } else if (data instanceof Object[]) {
            this.count = ((Object[]) data).length;
        }
    }
    
    // Constructeur error
    public JsonResponse(int code, String errorMessage) {
        this.status = "error";
        this.code = code;
        this.data = createErrorData(errorMessage);
    }
    
    private Map<String, String> createErrorData(String message) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("message", message);
        return errorData;
    }
    
    // Getters
    public String getStatus() { return status; }
    public int getCode() { return code; }
    public Object getData() { return data; }
    public Integer getCount() { return count; }
    
    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setCode(int code) { this.code = code; }
    public void setData(Object data) { this.data = data; }
    public void setCount(Integer count) { this.count = count; }
}
