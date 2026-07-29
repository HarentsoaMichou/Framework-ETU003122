package framework.service;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    
    private String view;
    private Map<String, Object> data;
    private Map<String, Object> session = new HashMap<>();
    
    public ModelView() {
        this.data = new HashMap<>();
    }
    
    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    public Map<String, Object> getSession() {
        return session;
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

    public void setSession(Map<String, Object> session) {
    this.session = session;
    }
    public void addSession(String key, Object value) {
        this.session.put(key, value);
    }
    public Object getSessionObject(String key) {
        return this.session.get(key);
    }
    public void removeSession(String key) {
        this.session.remove(key);
    }
    public boolean hasSessionKey(String key) {
        return this.session.containsKey(key);
    }
}