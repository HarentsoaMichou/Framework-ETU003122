package framework.servlet;

import framework.annotations.*;
import framework.service.FileUpload;
import framework.service.JsonResponse;
import framework.service.Mapping;
import framework.service.ModelView;
import framework.service.ScanController;
import framework.service.SessionHandler;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MultipartConfig(
    maxFileSize = 10485760,      // 10 MB
    maxRequestSize = 52428800,   // 50 MB
    fileSizeThreshold = 1048576  // 1 MB
)
public class FrontServlet extends HttpServlet {

    // Structure : urlMappings.get("GET").get("/users") → Mapping
    private Map<String, Map<String, Mapping>> urlMappings;
    private SessionHandler sessionHandler;   

    @Override
    public void init() throws ServletException {
        urlMappings = new HashMap<>();
        urlMappings.put("GET", new HashMap<>());
        urlMappings.put("POST", new HashMap<>());
        urlMappings.put("ALL", new HashMap<>()); // Pour @AnnotationMethode
        sessionHandler = new SessionHandler();  
        
        
        System.out.println("INITIALISATION DU FRAMEWORK");
        
        
        try {
            // Scanner toutes les classes avec @AnnotationClasse
            List<Class<?>> controllerClasses = ScanController.findAllClassesWithAnnotation(
                getServletContext(), 
                AnnotationClasse.class
            );
            
            System.out.println(" Classes contrôleurs trouvées : " + controllerClasses.size());
            
            // Analyser chaque classe contrôleur
            for (Class<?> clazz : controllerClasses) {
                analyzeController(clazz);
            }
            
            // Scanner aussi les classes sans @AnnotationClasse
            List<Class<?>> allClasses = ScanController.findAllClasses(getServletContext());
            for (Class<?> clazz : allClasses) {
                if (!clazz.isAnnotationPresent(AnnotationClasse.class)) {
                    scanMethodsOnly(clazz);
                }
            }
            
            
            System.out.println("MAPPINGS CRÉÉS");
            System.out.println("");
            printMappings();
            
            
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
            System.out.println("\n Analyse du contrôleur : " + clazz.getSimpleName());
            System.out.println("    URL de base : " + baseUrl);
        }
        
        // Scanner toutes les méthodes
        for (Method method : clazz.getDeclaredMethods()) {
            processMappingAnnotations(clazz, method, baseUrl);
        }
    }

    private void scanMethodsOnly(Class<?> clazz) {
        boolean hasAnnotatedMethod = false;
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasHttpAnnotation(method)) {
                if (!hasAnnotatedMethod) {
                    System.out.println("\n Analyse de la classe : " + clazz.getSimpleName());
                    hasAnnotatedMethod = true;
                }
                processMappingAnnotations(clazz, method, "");
            }
        }
    }

    private boolean hasHttpAnnotation(Method method) {
        return method.isAnnotationPresent(Get.class) ||
               method.isAnnotationPresent(Post.class) ||
               method.isAnnotationPresent(RequestMapping.class) ||
               method.isAnnotationPresent(AnnotationMethode.class);
    }

    private void processMappingAnnotations(Class<?> clazz, Method method, String baseUrl) {
        // 1. Vérifier @Get
        if (method.isAnnotationPresent(Get.class)) {
            Get getAnnotation = method.getAnnotation(Get.class);
            String methodUrl = getAnnotation.value();
            String fullUrl = baseUrl + methodUrl;
            addMapping(clazz, method, fullUrl, "GET");
        }
        
        // 2. Vérifier @Post
        if (method.isAnnotationPresent(Post.class)) {
            Post postAnnotation = method.getAnnotation(Post.class);
            String methodUrl = postAnnotation.value();
            String fullUrl = baseUrl + methodUrl;
            addMapping(clazz, method, fullUrl, "POST");
        }
        
        // 3. Vérifier @RequestMapping
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rmAnnotation = method.getAnnotation(RequestMapping.class);
            String methodUrl = rmAnnotation.value();
            String httpMethod = rmAnnotation.method().toUpperCase();
            String fullUrl = baseUrl + methodUrl;
            addMapping(clazz, method, fullUrl, httpMethod);
        }
        
        // 4. Vérifier @AnnotationMethode (rétrocompatibilité - accepte toutes les méthodes)
        if (method.isAnnotationPresent(AnnotationMethode.class)) {
            AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
            String methodUrl = annMethode.value();
            String fullUrl = baseUrl + methodUrl;
            addMapping(clazz, method, fullUrl, "ALL");
        }
    }

    private void addMapping(Class<?> clazz, Method method, String url, String httpMethod) {
        Mapping mapping = new Mapping();
        mapping.setClassName(clazz.getName());
        mapping.setMethod(method);
        mapping.setUrl(url);
        mapping.setHttpMethod(httpMethod);
        
        // Vérifier si le mapping existe déjà (conflit)
        Map<String, Mapping> methodMappings = urlMappings.get(httpMethod);
        if (methodMappings.containsKey(url)) {
            System.err.println("    ERREUR : Conflit de mapping détecté !");
            System.err.println("      URL : " + url + " avec méthode HTTP : " + httpMethod);
            System.err.println("      Déjà mappé vers : " + methodMappings.get(url).getClassName() + 
                             "." + methodMappings.get(url).getMethod().getName());
            System.err.println("      Tentative : " + clazz.getName() + "." + method.getName());
            throw new RuntimeException("Conflit de mapping : " + httpMethod + " " + url);
        }
        
        methodMappings.put(url, mapping);
        
        String icon = httpMethod.equals("GET") ? "" : 
                     httpMethod.equals("POST") ? "" : "";
        System.out.println("   " + icon + " " + httpMethod + " " + url + " → " + 
                         method.getName() + "() [" + method.getReturnType().getSimpleName() + "]");
    }

    private void printMappings() {
        int totalMappings = 0;
        
        if (!urlMappings.get("GET").isEmpty()) {
            System.out.println("\n GET Mappings :");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("GET").entrySet()) {
                Mapping m = entry.getValue();
                System.out.println("   " + entry.getKey() + " → " + 
                                 m.getClassName().substring(m.getClassName().lastIndexOf('.') + 1) + 
                                 "." + m.getMethod().getName() + "()");
                totalMappings++;
            }
        }
        
        if (!urlMappings.get("POST").isEmpty()) {
            System.out.println("\n POST Mappings :");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("POST").entrySet()) {
                Mapping m = entry.getValue();
                System.out.println("   " + entry.getKey() + " → " + 
                                 m.getClassName().substring(m.getClassName().lastIndexOf('.') + 1) + 
                                 "." + m.getMethod().getName() + "()");
                totalMappings++;
            }
        }
        
        if (!urlMappings.get("ALL").isEmpty()) {
            System.out.println("\n ALL Methods (@AnnotationMethode) :");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("ALL").entrySet()) {
                Mapping m = entry.getValue();
                System.out.println("   " + entry.getKey() + " → " + 
                                 m.getClassName().substring(m.getClassName().lastIndexOf('.') + 1) + 
                                 "." + m.getMethod().getName() + "()");
                totalMappings++;
            }
        }
        
        System.out.println("\n Total : " + totalMappings + " mapping(s)");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response, "POST");
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response, String httpMethod) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        System.out.println(" Requête reçue : " + httpMethod + " " + path);
        
        // 1. Chercher dans les mappings spécifiques à la méthode HTTP
        Mapping mapping = findMapping(path, httpMethod);
        
        // 2. Si pas trouvé, chercher dans les mappings ALL (@AnnotationMethode)
        if (mapping == null) {
            mapping = findMapping(path, "ALL");
        }
        
        if (mapping != null) {
            handleMappedUrl(request, response, path, mapping, null);
            return;
        }
        
        // 3. Vérifier si c'est une ressource statique
        URL resource = getServletContext().getResource(path);
        if (resource != null) {
        String dispatcherName = path.endsWith(".jsp") ? "jsp" : "default";
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(dispatcherName);
        if (dispatcher != null) {
            dispatcher.forward(request, response);
            return;
        }
    }
        
        // 4. Afficher la page 404
        show404Page(response, path, request, httpMethod);
    }

    private Mapping findMapping(String path, String httpMethod) {
        Map<String, Mapping> methodMappings = urlMappings.get(httpMethod);
        
        // 1. Correspondance exacte
        if (methodMappings.containsKey(path)) {
            return methodMappings.get(path);
        }
        
        // 2. Correspondance avec paramètres dynamiques
        for (Map.Entry<String, Mapping> entry : methodMappings.entrySet()) {
            Mapping mapping = entry.getValue();
            if (mapping.hasDynamicParams() && mapping.matchesUrl(path)) {
                return mapping;
            }
        }
        
        return null;
    }

    // pour détecter @Json
    private boolean isJsonMethod(Method method) {
        return method.isAnnotationPresent(Json.class);
    }

    private void handleMappedUrl(HttpServletRequest request, HttpServletResponse response, 
                            String path, Mapping mapping, Map<String, String> urlParams) 
        throws ServletException, IOException {
    
    Method method = mapping.getMethod();
    
    // Vérifier si la méthode retourne du JSON
    boolean isJson = isJsonMethod(method);
    
    if (urlParams == null && mapping.hasDynamicParams()) {
        urlParams = mapping.extractUrlParams(path);
    }
    
    try {
        Class<?> controllerClass = Class.forName(mapping.getClassName());
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        //  tracker les Maps de session injectées pour les resynchroniser après
        List<Map<String, Object>> injectedSessionMaps = new ArrayList<>();
        
        // Préparer les arguments (avec support @RequestBody pour JSON et @Session)
        Object[] methodArgs = prepareMethodArgumentsWithJson(method, urlParams, request, isJson, injectedSessionMaps);
        Object result = method.invoke(controllerInstance, methodArgs);
        
        //  resynchroniser les Maps @Session vers la HttpSession
        for (Map<String, Object> sessionMap : injectedSessionMaps) {
            sessionHandler.updateSessionData(request, sessionMap);
        }
        
        //  si le résultat est un ModelView, sync la session depuis le ModelView
        if (result instanceof ModelView) {
            sessionHandler.updateSessionFromModelView(request, (ModelView) result);
        }
        
        // Si c'est une méthode @Json, retourner du JSON
        if (isJson) {
            handleJsonResponse(response, result);
        } else {
            // Sinon, comportement normal (HTML)
            handleMethodResult(request, response, result, controllerClass, method, path);
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        if (isJson) {
            handleJsonError(response, e);
        } else {
            showErrorPage(response, e);
        }
    }
}

// Préparer les arguments avec support JSON, fichiers et sessions ()
private Object[] prepareMethodArgumentsWithJson(Method method, Map<String, String> urlParams, 
                                                HttpServletRequest request, boolean isJsonMethod,
                                                List<Map<String, Object>> injectedSessionMaps) 
        throws Exception {
    
    Class<?>[] paramTypes = method.getParameterTypes();
    java.lang.reflect.Parameter[] parameters = method.getParameters();
    Object[] args = new Object[paramTypes.length];
    
    // Vérifier si c'est un multipart request
    boolean isMultipart = request.getContentType() != null && 
                         request.getContentType().toLowerCase().startsWith("multipart/form-data");
    
    // Créer la Map complète de tous les paramètres
    Map<String, Object> allParams = isMultipart ? 
        buildParamsMapFromMultipart(request) : 
        buildAllParamsMap(urlParams, request);
    
    Map<String, String> remainingUrlParams = urlParams != null ? 
        new HashMap<>(urlParams) : new HashMap<>();
    
    System.out.println("\n Résolution des paramètres pour " + method.getName() + "()");
    if (isMultipart) {
        System.out.println("    Mode Multipart/Upload activé");
    }
    
    for (int i = 0; i < parameters.length; i++) {
        java.lang.reflect.Parameter param = parameters[i];
        Class<?> paramType = paramTypes[i];
        
        // 0.  : Vérifier si c'est un paramètre @Session
        if (param.isAnnotationPresent(Session.class)) {
            Map<String, Object> sessionData = sessionHandler.extractSessionData(request);
            args[i] = sessionData;
            injectedSessionMaps.add(sessionData);  // tracker pour resync après
            System.out.println("    @Session injectée : " + sessionData.size() + " attribut(s)");
            continue;
        }
        
        //  : Vérifier si c'est un ModelView (injecter la session dedans)
        if (paramType == ModelView.class) {
            ModelView mv = new ModelView();
            sessionHandler.injectSessionIntoModelView(request, mv);
            args[i] = mv;
            System.out.println("    ModelView injecté avec session");
            continue;
        }
        
        // 1. Vérifier si c'est un fichier uploadé (Part)
        if (paramType == Part.class) {
            String paramName = param.getName();
            Part part = request.getPart(paramName);
            args[i] = part;
            if (part != null) {
                System.out.println("    Part uploadé : " + paramName + 
                                 " (" + part.getSize() + " octets)");
            }
            continue;
        }
        
        // 2. NOUVEAU : Vérifier si c'est un FileUpload
        if (paramType == FileUpload.class) {
            String paramName = param.getName();
            Part part = request.getPart(paramName);
            if (part != null && part.getSize() > 0) {
                args[i] = new FileUpload(part);
                System.out.println("    FileUpload : " + ((FileUpload)args[i]).getFilename() + 
                                 " (" + part.getSize() + " octets)");
            } else {
                args[i] = null;
                System.out.println("    Aucun fichier uploadé pour : " + paramName);
            }
            continue;
        }
        
        // 3. NOUVEAU : Vérifier si c'est un tableau de FileUpload
        if (paramType == FileUpload[].class) {
            String paramName = param.getName();
            Collection<Part> parts = request.getParts().stream()
                .filter(p -> paramName.equals(p.getName()) && p.getSize() > 0)
                .collect(java.util.stream.Collectors.toList());
            
            FileUpload[] uploads = new FileUpload[parts.size()];
            int idx = 0;
            for (Part part : parts) {
                uploads[idx++] = new FileUpload(part);
            }
            args[i] = uploads;
            System.out.println("    FileUpload[] : " + uploads.length + " fichier(s)");
            continue;
        }
        
        // 4. NOUVEAU : Vérifier si c'est une List<FileUpload>
        if (paramType == List.class || paramType == java.util.ArrayList.class) {
            // Vérifier les génériques si possible
            java.lang.reflect.Type genericType = param.getParameterizedType();
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType pType = 
                    (java.lang.reflect.ParameterizedType) genericType;
                if (pType.getActualTypeArguments()[0] == FileUpload.class) {
                    String paramName = param.getName();
                    Collection<Part> parts = request.getParts().stream()
                        .filter(p -> paramName.equals(p.getName()) && p.getSize() > 0)
                        .collect(java.util.stream.Collectors.toList());
                    
                    List<FileUpload> uploads = new ArrayList<>();
                    for (Part part : parts) {
                        uploads.add(new FileUpload(part));
                    }
                    args[i] = uploads;
                    System.out.println("    List<FileUpload> : " + uploads.size() + " fichier(s)");
                    continue;
                }
            }
        }
        
        // 5. Vérifier si c'est un @RequestBody
        if (param.isAnnotationPresent(RequestBody.class)) {
            String jsonBody = readRequestBody(request);
            Object parsedObject = parseJsonToObject(jsonBody, paramType);
            args[i] = parsedObject;
            System.out.println("    @RequestBody parsé : " + paramType.getSimpleName());
            continue;
        }
        
        // 6. Vérifier si c'est un paramètre Map<String, Object>
        if (isParamsMap(paramType, param)) {
            args[i] = allParams;
            System.out.println("    Map<String, Object> : " + allParams.size() + " paramètre(s)");
            continue;
        }
        
        // 7. Vérifier si c'est un objet custom à binder
        if (isCustomObject(paramType)) {
            if (isJsonContentType(request) && !hasRequestBodyParam(method)) {
                String jsonBody = readRequestBody(request);
                Object parsedObject = parseJsonToObject(jsonBody, paramType);
                args[i] = parsedObject;
                System.out.println("    Objet bindé depuis JSON : " + paramType.getSimpleName());
            } else {
                Object boundObject = bindObject(paramType, request, allParams);
                args[i] = boundObject;
                System.out.println("    Objet bindé : " + paramType.getSimpleName());
            }
            continue;
        }
        
        // 8. Traitement normal des paramètres primitifs
        String paramValue = null;
        
        Param paramAnnotation = param.getAnnotation(Param.class);
        if (paramAnnotation != null) {
            String paramName = paramAnnotation.value();
            if (remainingUrlParams.containsKey(paramName)) {
                paramValue = remainingUrlParams.get(paramName);
                remainingUrlParams.remove(paramName);
            } else {
                paramValue = request.getParameter(paramName);
            }
        } else {
            String paramName = param.getName();
            if (remainingUrlParams.containsKey(paramName)) {
                paramValue = remainingUrlParams.get(paramName);
                remainingUrlParams.remove(paramName);
            } else if (!remainingUrlParams.isEmpty()) {
                String firstKey = remainingUrlParams.keySet().iterator().next();
                paramValue = remainingUrlParams.get(firstKey);
                remainingUrlParams.remove(firstKey);
            } else {
                paramValue = request.getParameter(paramName);
            }
        }
        
        args[i] = paramValue != null ? convertParameter(paramValue, paramType) : getDefaultValue(paramType);
    }
    
    return args;
}


// Construire la Map depuis un multipart request
private Map<String, Object> buildParamsMapFromMultipart(HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<>();
    
    // Récupérer tous les parts
    Collection<Part> parts = request.getParts();
    
    for (Part part : parts) {
        String name = part.getName();
        
        // Si c'est un fichier (a un filename)
        if (part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
            if (part.getSize() > 0) {
                // Ne pas mettre dans la map, sera géré par FileUpload
                continue;
            }
        } else {
            // C'est un champ texte normal
            String value = new BufferedReader(
                new InputStreamReader(part.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                .lines()
                .collect(java.util.stream.Collectors.joining("\n"));
            
            params.put(name, smartConvert(value));
        }
    }
    
    return params;
}

// Vérifier si la requête a un Content-Type JSON
private boolean isJsonContentType(HttpServletRequest request) {
    String contentType = request.getContentType();
    return contentType != null && contentType.toLowerCase().contains("application/json");
}

// Vérifier si la méthode a un paramètre @RequestBody
private boolean hasRequestBodyParam(Method method) {
    for (java.lang.reflect.Parameter param : method.getParameters()) {
        if (param.isAnnotationPresent(RequestBody.class)) {
            return true;
        }
    }
    return false;
}

// Lire le body de la requête
private String readRequestBody(HttpServletRequest request) throws IOException {
    StringBuilder buffer = new StringBuilder();
    java.io.BufferedReader reader = request.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
        buffer.append(line);
    }
    return buffer.toString();
}

// Parser JSON vers un objet avec Jackson
private Object parseJsonToObject(String json, Class<?> targetClass) throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    
    // Configuration pour gérer les dates
    mapper.findAndRegisterModules();
    mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    return mapper.readValue(json, targetClass);
}

// Parser JSON vers une Map
private Map<String, Object> parseJsonToMap(String json) throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
}

// Gérer la réponse JSON
private void handleJsonResponse(HttpServletResponse response, Object result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    mapper.findAndRegisterModules();
    mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    JsonResponse jsonResponse;
    
    // Si le résultat est déjà une JsonResponse, l'utiliser directement
    if (result instanceof JsonResponse) {
        jsonResponse = (JsonResponse) result;
    } else {
        // Sinon, créer une JsonResponse avec le résultat
        jsonResponse = new JsonResponse(200, result);
    }
    
    // Définir le status code HTTP
    response.setStatus(jsonResponse.getCode());
    
    // Écrire le JSON
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse);
    response.getWriter().write(json);
    
    System.out.println(" [JSON] Réponse envoyée : " + jsonResponse.getStatus() + 
                      " (code " + jsonResponse.getCode() + ")");
}

// Gérer les erreurs en JSON
private void handleJsonError(HttpServletResponse response, Exception e) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    
    JsonResponse errorResponse = new JsonResponse(500, "Erreur serveur : " + e.getMessage());
    response.setStatus(500);
    
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
    response.getWriter().write(json);
    
    System.out.println(" [JSON] Erreur : " + e.getMessage());
}

    // Les méthodes prepareMethodArguments, convertParameter, getDefaultValue, 
    // handleMethodResult, showErrorPage restent identiques au Sprint 4
    
    
private Object[] prepareMethodArguments(Method method, Map<String, String> urlParams, 
                                       HttpServletRequest request) {
    Class<?>[] paramTypes = method.getParameterTypes();
    java.lang.reflect.Parameter[] parameters = method.getParameters();
    Object[] args = new Object[paramTypes.length];
    
    // Créer la Map complète de tous les paramètres
    Map<String, Object> allParams = buildAllParamsMap(urlParams, request);
    
    Map<String, String> remainingUrlParams = urlParams != null ? 
        new HashMap<>(urlParams) : new HashMap<>();
    
    System.out.println("\n Résolution des paramètres pour " + method.getName() + "()");
    if (!allParams.isEmpty()) {
        System.out.println("    Paramètres disponibles : " + allParams.keySet());
    }
    
    for (int i = 0; i < parameters.length; i++) {
        java.lang.reflect.Parameter param = parameters[i];
        Class<?> paramType = paramTypes[i];
        
        // 1. Vérifier si c'est un paramètre Map<String, Object>
        if (isParamsMap(paramType, param)) {
            args[i] = allParams;
            System.out.println("    Map<String, Object> injectée avec " + allParams.size() + " paramètre(s)");
            continue;
        }
        
        // 2. NOUVEAU : Vérifier si c'est un objet custom à binder
        if (isCustomObject(paramType)) {
            try {
                Object boundObject = bindObject(paramType, request, allParams);
                args[i] = boundObject;
                System.out.println("    Objet bindé : " + paramType.getSimpleName() + 
                                 " avec " + countFilledFields(boundObject) + " champ(s) rempli(s)");
            } catch (Exception e) {
                System.err.println("    Erreur de binding pour " + paramType.getSimpleName() + " : " + e.getMessage());
                throw new RuntimeException("Erreur de binding : " + e.getMessage(), e);
            }
            continue;
        }
        
        // 3. Sinon, traitement normal des paramètres primitifs
        String paramValue = null;
        String paramName = null;
        
        Param paramAnnotation = param.getAnnotation(Param.class);
        
        if (paramAnnotation != null) {
            paramName = paramAnnotation.value();
            if (remainingUrlParams.containsKey(paramName)) {
                paramValue = remainingUrlParams.get(paramName);
                remainingUrlParams.remove(paramName);
            } else {
                paramValue = request.getParameter(paramName);
            }
        } else {
            paramName = param.getName();
            if (remainingUrlParams.containsKey(paramName)) {
                paramValue = remainingUrlParams.get(paramName);
                remainingUrlParams.remove(paramName);
            } else if (!remainingUrlParams.isEmpty()) {
                String firstKey = remainingUrlParams.keySet().iterator().next();
                paramValue = remainingUrlParams.get(firstKey);
                remainingUrlParams.remove(firstKey);
            } else {
                paramValue = request.getParameter(paramName);
            }
        }
        
        if (paramValue != null) {
            args[i] = convertParameter(paramValue, paramType);
        } else {
            args[i] = getDefaultValue(paramType);
        }
    }
    
    System.out.println();
    return args;
}

/**
 * Vérifier si un type est un objet custom (pas un type standard)
 */
private boolean isCustomObject(Class<?> type) {
    // Types standards à exclure
    if (type.isPrimitive()) return false;
    if (type == String.class) return false;
    if (type == Integer.class) return false;
    if (type == Long.class) return false;
    if (type == Double.class) return false;
    if (type == Float.class) return false;
    if (type == Boolean.class) return false;
    if (type == Character.class) return false;
    if (type == Byte.class) return false;
    if (type == Short.class) return false;
    if (type == java.util.Date.class) return false;
    if (type == java.time.LocalDate.class) return false;
    if (type == java.time.LocalDateTime.class) return false;
    if (Map.class.isAssignableFrom(type)) return false;
    if (type.isArray()) return false;
    
    // Si c'est dans java.* ou jakarta.* → pas custom
    if (type.getName().startsWith("java.")) return false;
    if (type.getName().startsWith("jakarta.")) return false;
    
    // Sinon, c'est un objet custom
    return true;
}

/**
 * Binder un objet custom à partir des paramètres de la requête
 */
private Object bindObject(Class<?> type, HttpServletRequest request, Map<String, Object> allParams) 
        throws Exception {
    
    System.out.println("    Binding de " + type.getSimpleName() + "...");
    
    // Créer une instance avec le constructeur sans arguments
    Object instance = type.getDeclaredConstructor().newInstance();
    
    // Récupérer tous les champs de la classe
    java.lang.reflect.Field[] fields = type.getDeclaredFields();
    
    for (java.lang.reflect.Field field : fields) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        
        // Essayer de trouver le setter
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        java.lang.reflect.Method setter = findSetter(type, setterName, fieldType);
        
        if (setter == null) {
            System.out.println("       Pas de setter pour " + fieldName);
            continue;
        }
        
        // Chercher la valeur du paramètre
        Object value = findParameterValue(fieldName, fieldType, request, allParams);
        
        if (value != null) {
            try {
                setter.invoke(instance, value);
                System.out.println("       " + fieldName + " = " + value + " (" + fieldType.getSimpleName() + ")");
            } catch (Exception e) {
                System.err.println("       Erreur setter " + fieldName + " : " + e.getMessage());
            }
        }
    }
    
    return instance;
}

/**
 * Trouver un setter pour un champ
 */
private java.lang.reflect.Method findSetter(Class<?> clazz, String setterName, Class<?> paramType) {
    try {
        return clazz.getMethod(setterName, paramType);
    } catch (NoSuchMethodException e) {
        return null;
    }
}

/**
 * Trouver la valeur d'un paramètre pour un champ
 * Supporte : camelCase, snake_case, et notation pointée (objet.champ)
 */
private Object findParameterValue(String fieldName, Class<?> fieldType, 
                                  HttpServletRequest request, Map<String, Object> allParams) {
    
    // 1. Si c'est un objet custom imbriqué → binding récursif avec notation pointée
    if (isCustomObject(fieldType)) {
        try {
            return bindNestedObject(fieldName, fieldType, request, allParams);
        } catch (Exception e) {
            System.err.println("       Erreur binding objet imbriqué " + fieldName + " : " + e.getMessage());
            return null;
        }
    }
    
    // 2. Essayer avec le nom exact (camelCase)
    String paramValue = request.getParameter(fieldName);
    if (paramValue != null) {
        return convertToType(paramValue, fieldType);
    }
    
    // 3. Essayer avec snake_case (idDepartement → id_departement)
    String snakeCaseName = camelToSnakeCase(fieldName);
    paramValue = request.getParameter(snakeCaseName);
    if (paramValue != null) {
        return convertToType(paramValue, fieldType);
    }
    
    // 4. Essayer dans allParams (déjà converti)
    if (allParams.containsKey(fieldName)) {
        return convertToType(String.valueOf(allParams.get(fieldName)), fieldType);
    }
    
    if (allParams.containsKey(snakeCaseName)) {
        return convertToType(String.valueOf(allParams.get(snakeCaseName)), fieldType);
    }
    
    return null;
}

/**
 * Binder un objet imbriqué avec notation pointée (ex: departement.id)
 */
private Object bindNestedObject(String prefix, Class<?> type, 
                                HttpServletRequest request, Map<String, Object> allParams) 
        throws Exception {
    
    System.out.println("       Binding objet imbriqué : " + prefix + " (" + type.getSimpleName() + ")");
    
    Object instance = type.getDeclaredConstructor().newInstance();
    java.lang.reflect.Field[] fields = type.getDeclaredFields();
    
    for (java.lang.reflect.Field field : fields) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        
        // Chercher avec notation pointée : prefix.fieldName
        String dottedName = prefix + "." + fieldName;
        String paramValue = request.getParameter(dottedName);
        
        if (paramValue != null) {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            java.lang.reflect.Method setter = findSetter(type, setterName, fieldType);
            
            if (setter != null) {
                Object value = convertToType(paramValue, fieldType);
                setter.invoke(instance, value);
                System.out.println("          " + dottedName + " = " + value);
            }
        }
    }
    
    return instance;
}

/**
 * Convertir camelCase en snake_case
 */
private String camelToSnakeCase(String camelCase) {
    return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
}

/**
 * Convertir une String vers un type spécifique
 */
private Object convertToType(String value, Class<?> targetType) {
    if (value == null || value.isEmpty()) {
        return null;
    }
    
    try {
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == java.util.Date.class) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(value);
        } else if (targetType == java.time.LocalDate.class) {
            return java.time.LocalDate.parse(value);
        } else if (targetType == java.time.LocalDateTime.class) {
            return java.time.LocalDateTime.parse(value);
        }
    } catch (Exception e) {
        System.err.println("       Erreur conversion : " + value + " vers " + targetType.getName());
    }
    
    return null;
}

/**
 * Compter les champs non-null d'un objet (pour logging)
 */
private int countFilledFields(Object obj) {
    if (obj == null) return 0;
    
    int count = 0;
    java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
    
    for (java.lang.reflect.Field field : fields) {
        field.setAccessible(true);
        try {
            Object value = field.get(obj);
            if (value != null) {
                // Pour les primitifs, vérifier si différent de la valeur par défaut
                if (field.getType().isPrimitive()) {
                    if (field.getType() == int.class && (int) value != 0) count++;
                    else if (field.getType() == long.class && (long) value != 0L) count++;
                    else if (field.getType() == double.class && (double) value != 0.0) count++;
                    else if (field.getType() == float.class && (float) value != 0.0f) count++;
                    else if (field.getType() == boolean.class && (boolean) value) count++;
                } else {
                    count++;
                }
            }
        } catch (IllegalAccessException e) {
            // Ignorer
        }
    }
    
    return count;
}

/**
 * Vérifier si un paramètre est de type Map<String, Object>
 */
private boolean isParamsMap(Class<?> paramType, java.lang.reflect.Parameter param) {
    if (!Map.class.isAssignableFrom(paramType)) {
        return false;
    }
    
    // Vérifier les types génériques si possible
    java.lang.reflect.Type genericType = param.getParameterizedType();
    if (genericType instanceof java.lang.reflect.ParameterizedType) {
        java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) genericType;
        java.lang.reflect.Type[] typeArgs = pType.getActualTypeArguments();
        
        // Vérifier si c'est Map<String, Object>
        if (typeArgs.length == 2) {
            boolean isStringKey = typeArgs[0].equals(String.class);
            boolean isObjectValue = typeArgs[1].equals(Object.class);
            return isStringKey && isObjectValue;
        }
    }
    
    // Par défaut, accepter toute Map
    return true;
}

/**
 * Construire la Map complète de tous les paramètres avec conversion automatique
 */
private Map<String, Object> buildAllParamsMap(Map<String, String> urlParams, 
                                              HttpServletRequest request) {
    Map<String, Object> allParams = new HashMap<>();
    
    // 1. Ajouter les paramètres d'URL dynamiques {id}, {userId}, etc.
    if (urlParams != null) {
        for (Map.Entry<String, String> entry : urlParams.entrySet()) {
            Object convertedValue = smartConvert(entry.getValue());
            allParams.put(entry.getKey(), convertedValue);
            System.out.println("    URL param : " + entry.getKey() + " = " + convertedValue + 
                             " (" + convertedValue.getClass().getSimpleName() + ")");
        }
    }
    
    // 2. Ajouter les paramètres de requête (query string + form data)
    java.util.Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
        String paramName = paramNames.nextElement();
        String[] paramValues = request.getParameterValues(paramName);
        
        if (paramValues.length == 1) {
            // Un seul paramètre : conversion intelligente
            Object convertedValue = smartConvert(paramValues[0]);
            allParams.put(paramName, convertedValue);
        } else {
            // Plusieurs paramètres avec le même nom : créer un tableau
            Object[] convertedArray = new Object[paramValues.length];
            for (int i = 0; i < paramValues.length; i++) {
                convertedArray[i] = smartConvert(paramValues[i]);
            }
            allParams.put(paramName, convertedArray);
            System.out.println("    Multi param : " + paramName + " = " + 
                             java.util.Arrays.toString(convertedArray));
        }
    }
    
    return allParams;
}

/**
 * Conversion intelligente automatique
 * Essaie de détecter le type et convertir
 */
private Object smartConvert(String value) {
    if (value == null || value.isEmpty()) {
        return value;
    }
    
    // 1. Essayer boolean
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
        return Boolean.parseBoolean(value);
    }
    
    // 2. Essayer int
    try {
        // Vérifier que c'est un entier pur (pas de point)
        if (!value.contains(".") && !value.contains(",")) {
            return Integer.parseInt(value);
        }
    } catch (NumberFormatException e) {
        // Pas un int, continuer
    }
    
    // 3. Essayer double
    try {
        if (value.contains(".") || value.contains(",")) {
            return Double.parseDouble(value.replace(",", "."));
        }
    } catch (NumberFormatException e) {
        // Pas un double, continuer
    }
    
    // 4. Essayer date (format ISO : yyyy-MM-dd)
    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
        try {
            return java.time.LocalDate.parse(value);
        } catch (Exception e) {
            // Pas une date valide
        }
    }
    
    // 5. Par défaut : String
    return value;
}

// Les autres méthodes restent identiques (convertParameter, getDefaultValue, etc.)

    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) return null;
        try {
            if (targetType == String.class) return value;
            if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
            if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
            if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
            if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
            if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }

    private void handleMethodResult(HttpServletRequest request, HttpServletResponse response,
                                    Object result, Class<?> controllerClass, Method method, String path)
            throws ServletException, IOException {
        
        if (result instanceof String) {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE html><html><head><title>Résultat</title>");
            out.println("<style>body{font-family:Arial;margin:40px;background:#f5f5f5}");
            out.println(".container{background:white;padding:30px;border-radius:10px}</style></head><body>");
            out.println("<div class='container'><h1> Succès</h1>");
            out.println("<div>" + result + "</div></div></body></html>");
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            if (mv.getView() != null && !mv.getView().isEmpty()) {
                // Transférer les données du ModelView vers les attributs de la requête
                if (mv.getData() != null) {
                    for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                }
                //  : exposer aussi les attributs de session à la JSP
                if (mv.getSession() != null) {
                    for (Map.Entry<String, Object> entry : mv.getSession().entrySet()) {
                        request.setAttribute("session_" + entry.getKey(), entry.getValue());
                    }
                }
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/" + mv.getView());
                dispatcher.forward(request, response);
            }
        }
    }

    private void showErrorPage(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println("<h1> Erreur</h1><p>" + e.getMessage() + "</p>");
    }

    private void show404Page(HttpServletResponse response, String requestedUrl, 
                            HttpServletRequest request, String httpMethod) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html><html><head><title>404 - Not Found</title>");
        out.println("<style>");
        out.println("body{font-family:Arial;margin:40px;background:#f5f5f5}");
        out.println(".container{background:white;padding:30px;border-radius:10px;max-width:900px;margin:0 auto}");
        out.println("h1{color:#f44336}.error{padding:20px;background:#ffebee;border-left:4px solid #f44336;margin:20px 0}");
        out.println(".routes{margin-top:30px}.route-item{padding:15px;background:#e3f2fd;margin:10px 0;border-radius:5px}");
        out.println(".method{display:inline-block;padding:5px 10px;border-radius:3px;font-weight:bold;margin-right:10px}");
        out.println(".get{background:#4caf50;color:white}.post{background:#2196f3;color:white}.all{background:#9e9e9e;color:white}");
        out.println("</style></head><body>");
        out.println("<div class='container'>");
        out.println("<h1> 404 - Route non trouvée</h1>");
        out.println("<div class='error'><strong>Requête :</strong> " + httpMethod + " " + requestedUrl + "</div>");
        
        out.println("<div class='routes'><h2> Routes disponibles :</h2>");
        
        // Afficher GET
        if (!urlMappings.get("GET").isEmpty()) {
            out.println("<h3> GET</h3>");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("GET").entrySet()) {
                printRoute(out, "GET", entry.getKey(), entry.getValue(), request);
            }
        }
        
        // Afficher POST
        if (!urlMappings.get("POST").isEmpty()) {
            out.println("<h3> POST</h3>");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("POST").entrySet()) {
                printRoute(out, "POST", entry.getKey(), entry.getValue(), request);
            }
        }
        
        // Afficher ALL
        if (!urlMappings.get("ALL").isEmpty()) {
            out.println("<h3> ALL Methods</h3>");
            for (Map.Entry<String, Mapping> entry : urlMappings.get("ALL").entrySet()) {
                printRoute(out, "ALL", entry.getKey(), entry.getValue(), request);
            }
        }
        
        out.println("</div></div></body></html>");
    }

    private void printRoute(PrintWriter out, String method, String url, Mapping mapping, HttpServletRequest request) {
        String methodClass = method.equals("GET") ? "get" : method.equals("POST") ? "post" : "all";
        out.println("<div class='route-item'>");
        out.println("<span class='method " + methodClass + "'>" + method + "</span>");
        out.println("<a href='" + request.getContextPath() + url + "'>" + url + "</a>");
        out.println(" → " + mapping.getClassName().substring(mapping.getClassName().lastIndexOf('.') + 1) + 
                   "." + mapping.getMethod().getName() + "()");
        out.println("</div>");
    }
}