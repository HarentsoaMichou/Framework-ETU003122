package framework.servlet;

import framework.annotations.AnnotationClasse;
import framework.annotations.AnnotationMethode;
import framework.service.Mapping;
import framework.service.ModelView;
import framework.service.ScanController;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontServlet extends HttpServlet {

    private Map<String, Mapping> urlMappings;

    @Override
    public void init() throws ServletException {
        urlMappings = new HashMap<>();
        
        try {
            // Scanner toutes les classes avec @AnnotationClasse
            List<Class<?>> controllerClasses = ScanController.findAllClassesWithAnnotation(
                getServletContext(), 
                AnnotationClasse.class
            );
            
            System.out.println("Classes controleurs trouvees : " + controllerClasses.size());
            
            // Analyser chaque classe controleur
            for (Class<?> clazz : controllerClasses) {
                analyzeController(clazz);
            }
            
            // Scanner aussi les classes sans @AnnotationClasse mais avec des methodes @AnnotationMethode
            List<Class<?>> allClasses = ScanController.findAllClasses(getServletContext());
            for (Class<?> clazz : allClasses) {
                if (!clazz.isAnnotationPresent(AnnotationClasse.class)) {
                    scanMethodsOnly(clazz);
                }
            }
            
            for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
                Mapping mapping = entry.getValue();
                System.out.println(entry.getKey() + 
                                 mapping.getClassName() + "." + 
                                 mapping.getMethod().getName() + "()");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur lors du scan des annotations", e);
        }
    }

    private void analyzeController(Class<?> clazz) {
        String baseUrl = "";
        
        if (clazz.isAnnotationPresent(AnnotationClasse.class)) {
            AnnotationClasse annClasse = clazz.getAnnotation(AnnotationClasse.class);
            baseUrl = annClasse.value();
            System.out.println("\n Analyse du controleur : " + clazz.getSimpleName());
            System.out.println("  URL de base : " + baseUrl);
        }
        
        // Scanner toutes les methodes annotees
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AnnotationMethode.class)) {
                AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
                String methodUrl = annMethode.value();
                
                // Construire l'URL complète
                String fullUrl = baseUrl + methodUrl;
                
                // Creer le mapping
                Mapping mapping = new Mapping();
                mapping.setClassName(clazz.getName());
                mapping.setMethod(method);
                mapping.setUrl(fullUrl);
                
                urlMappings.put(fullUrl, mapping);
                
                System.out.println("   Methode mappee : " + method.getName() + 
                                 "() → " + fullUrl + 
                                 " Type retour: " + method.getReturnType().getSimpleName() + "]");
            }
        }
    }

    private void scanMethodsOnly(Class<?> clazz) {
        boolean hasAnnotatedMethod = false;
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AnnotationMethode.class)) {
                if (!hasAnnotatedMethod) {
                    System.out.println("\n Analyse de la classe : " + clazz.getSimpleName());
                    hasAnnotatedMethod = true;
                }
                
                AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
                String methodUrl = annMethode.value();
                
                Mapping mapping = new Mapping();
                mapping.setClassName(clazz.getName());
                mapping.setMethod(method);
                mapping.setUrl(methodUrl);
                
                urlMappings.put(methodUrl, mapping);
                
                System.out.println("  Methode mappee : " + method.getName() + 
                                 "() → " + methodUrl + 
                                 " [Type retour: " + method.getReturnType().getSimpleName() + "]");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    
    String path = request.getRequestURI().substring(request.getContextPath().length());
    
    System.out.println(" Requête reçue : " + path);
    
    // 1. Chercher une correspondance exacte
    if (urlMappings.containsKey(path)) {
        handleMappedUrl(request, response, path, urlMappings.get(path), null);
        return;
    }
    
    // 2. Chercher une correspondance avec paramètres dynamiques
    for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
        Mapping mapping = entry.getValue();
        if (mapping.hasDynamicParams() && mapping.matchesUrl(path)) {
            Map<String, String> urlParams = mapping.extractUrlParams(path);
            handleMappedUrl(request, response, path, mapping, urlParams);
            return;
        }
    }
    
    // 3. Vérifier si c'est une ressource statique
    URL resource = getServletContext().getResource(path);
    if (resource != null) {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        if (defaultDispatcher != null) {
            defaultDispatcher.forward(request, response);
            return;
        }
    }
    
    // 4. Afficher la page 404
    show404Page(response, path, request);
}

    private void handleMappedUrl(HttpServletRequest request, HttpServletResponse response, 
                            String path, Mapping mapping, Map<String, String> urlParams) 
        throws ServletException, IOException {
    
    Method method = mapping.getMethod();
    
    try {
        // Créer une instance du contrôleur
        Class<?> controllerClass = Class.forName(mapping.getClassName());
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        // Préparer les arguments de la méthode
        Object[] methodArgs = prepareMethodArguments(method, urlParams);
        
        // Invoquer la méthode avec les arguments
        Object result = method.invoke(controllerInstance, methodArgs);
        
        // Traiter le résultat selon son type
        handleMethodResult(request, response, result, controllerClass, method, path);
        
    } catch (Exception e) {
        e.printStackTrace();
        showErrorPage(response, e);
    }
}

// Nouvelle méthode pour préparer les arguments de la méthode
private Object[] prepareMethodArguments(Method method, Map<String, String> urlParams) {
    Class<?>[] paramTypes = method.getParameterTypes();
    Object[] args = new Object[paramTypes.length];
    
    if (urlParams == null || urlParams.isEmpty()) {
        return args;
    }
    
    // Pour l'instant, on suppose que les paramètres de la méthode
    // correspondent aux paramètres de l'URL dans l'ordre
    int paramIndex = 0;
    for (String paramValue : urlParams.values()) {
        if (paramIndex < paramTypes.length) {
            args[paramIndex] = convertParameter(paramValue, paramTypes[paramIndex]);
            paramIndex++;
        }
    }
    
    return args;
}

// Nouvelle méthode pour convertir les types
private Object convertParameter(String value, Class<?> targetType) {
    if (value == null) {
        return null;
    }
    
    try {
        // String
        if (targetType == String.class) {
            return value;
        }
        // int / Integer
        else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        // long / Long
        else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        // double / Double
        else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        // float / Float
        else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        // boolean / Boolean
        else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Date (format: yyyy-MM-dd)
        else if (targetType == java.util.Date.class) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(value);
        }
        // LocalDate
        else if (targetType == java.time.LocalDate.class) {
            return java.time.LocalDate.parse(value);
        }
        // LocalDateTime
        else if (targetType == java.time.LocalDateTime.class) {
            return java.time.LocalDateTime.parse(value);
        }
        // Par défaut, retourner la String
        else {
            return value;
        }
    } catch (Exception e) {
        System.err.println(" Erreur de conversion : " + value + " vers " + targetType.getName());
        return null;
    }
}


// Extraire la logique de traitement du résultat
private void handleMethodResult(HttpServletRequest request, HttpServletResponse response,
                                Object result, Class<?> controllerClass, Method method, String path)
        throws ServletException, IOException {
    
    if (result instanceof String) {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Résultat</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }");
        out.println(".container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        out.println("h1 { color: #4CAF50; }");
        out.println(".result { padding: 20px; background: #e8f5e9; border-left: 4px solid #4CAF50; margin-top: 20px; }");
        out.println(".info { color: #666; margin-top: 20px; font-size: 14px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h1> Méthode exécutée avec succès</h1>");
        out.println("<div class='info'>");
        out.println("<strong>URL :</strong> " + path + "<br>");
        out.println("<strong>Contrôleur :</strong> " + controllerClass.getSimpleName() + "<br>");
        out.println("<strong>Méthode :</strong> " + method.getName() + "()");
        out.println("</div>");
        out.println("<div class='result'>");
        out.println(result);
        out.println("</div>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
        
        System.out.println(" [String] Méthode exécutée : " + controllerClass.getSimpleName() + "." + method.getName());
        
    } else if (result instanceof ModelView) {
        ModelView modelView = (ModelView) result;
        
        if (modelView.getView() != null && !modelView.getView().isEmpty()) {
            String viewPath = "/" + modelView.getView();
            
            System.out.println(" [ModelView] Forward vers : " + viewPath);
            
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(viewPath);
            dispatcher.forward(request, response);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<!DOCTYPE html><html><body>");
            response.getWriter().println("<h1> Erreur</h1>");
            response.getWriter().println("<p>Aucune vue spécifiée dans ModelView !</p>");
            response.getWriter().println("</body></html>");
            
            System.out.println(" ModelView sans vue définie");
        }
        
    } else {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println("<!DOCTYPE html><html><body>");
        response.getWriter().println("<h1> Type de retour non supporté</h1>");
        response.getWriter().println("<p>La méthode doit retourner String ou ModelView</p>");
        response.getWriter().println("<p>Type retourné : " + (result != null ? result.getClass().getName() : "null") + "</p>");
        response.getWriter().println("</body></html>");
        
        System.out.println(" Type de retour non supporté : " + (result != null ? result.getClass().getName() : "null"));
    }
}

private void showErrorPage(HttpServletResponse response, Exception e) throws IOException {
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println("<!DOCTYPE html>");
    out.println("<html><head><title>Erreur</title></head>");
    out.println("<body>");
    out.println("<h1> Erreur lors de l'exécution</h1>");
    out.println("<p>" + e.getMessage() + "</p>");
    out.println("<pre>" + e.getClass().getName() + "</pre>");
    out.println("</body></html>");
    
    System.out.println(" Erreur : " + e.getMessage());
}
    private void show404Page(HttpServletResponse response, String requestedUrl, HttpServletRequest request) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>404 - Route non trouvee</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h1> 404 - Route non trouvee</h1>");
        out.println("<div class='error'>");
        out.println("<strong>URL demandee :</strong> " + requestedUrl);
        out.println("</div>");
        
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
        
        System.out.println(" Route non trouvee : " + requestedUrl);
    }
}