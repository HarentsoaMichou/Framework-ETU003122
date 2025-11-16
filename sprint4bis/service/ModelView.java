package framework.service;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    
    private String view;
    private Map<String, Object> data;
    
    public ModelView() {
        this.data = new HashMap<>();
    }
    
    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public void addObject(String key, Object value) {
        this.data.put(key, value);
    }
}