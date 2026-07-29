package framework.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionHandler {

    /**
     * Extrait tous les attributs de la session HTTP dans une Map.
     */
    public Map<String, Object> extractSessionData(HttpServletRequest request) {
        Map<String, Object> sessionData = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String key = attributeNames.nextElement();
                Object value = session.getAttribute(key);
                sessionData.put(key, value);
            }
        }
        return sessionData;
    }

    /**
     * Met à jour la session HTTP à partir des données d'une Map.
     * Si une valeur est null, l'attribut est supprimé de la session.
     */
    public void updateSessionData(HttpServletRequest request, Map<String, Object> sessionData) {
        if (sessionData == null) {
            return;
        }
        
        // true pour créer la session si elle n'existe pas encore
        HttpSession session = request.getSession(true);
        
        for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                session.removeAttribute(key);
            } else {
                session.setAttribute(key, value);
            }
        }
    }

    public void injectSessionIntoModelView(HttpServletRequest request, ModelView modelView) {
        if (modelView != null) {
            modelView.setSession(extractSessionData(request));
        }
    }

    public void updateSessionFromModelView(HttpServletRequest request, ModelView modelView) {
        if (modelView != null && modelView.getSession() != null) {
            updateSessionData(request, modelView.getSession());
        }
    }
}
