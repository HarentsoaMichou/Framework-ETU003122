package framework.scanner;

import framework.annotations.AnnotationClasse;
import framework.annotations.AnnotationMethode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class AnnotationScanner extends HttpServlet {

    // Map pour stocker les mappings URL -> Methode
    private Map<String, MappingInfo> urlMappings = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            // Scanner le package où se trouvent vos contrôleurs
            scanPackage("annotations");
            
            for (Map.Entry<String, MappingInfo> entry : urlMappings.entrySet()) {
                System.out.println(entry.getKey() + 
                                 entry.getValue().className + "." + 
                                 entry.getValue().method.getName() + "()");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur lors du scan des annotations", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Recuperer l'URL demandee
        String requestedUrl = request.getRequestURI().substring(request.getContextPath().length());
        
        System.out.println("Requete reçue : " + requestedUrl);
        
        // Chercher le mapping correspondant
        MappingInfo mapping = urlMappings.get(requestedUrl);
        
        if (mapping != null) {
            try {
                // Invoquer la methode par reflexion
                Object controllerInstance = mapping.controllerClass.getDeclaredConstructor().newInstance();
                Object result = mapping.method.invoke(controllerInstance);
                
                // Si le resultat est une String, l'afficher
                if (result instanceof String) {
                    response.setContentType("text/html;charset=UTF-8");
                    PrintWriter out = response.getWriter();
                    out.println("<!DOCTYPE html>");
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Resultat</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<div class='container'>");
                    out.println("<h1>Methode executee avec succès</h1>");
                    out.println("<div class='info'>");
                    out.println("<strong>URL :</strong> " + requestedUrl + "<br>");
                    out.println("<strong>Contrôleur :</strong> " + mapping.className + "<br>");
                    out.println("<strong>Methode :</strong> " + mapping.method.getName() + "()");
                    out.println("</div>");
                    out.println("<div class='result'>");
                    out.println("<h3>Resultat :</h3>");
                    out.println("<p>" + result + "</p>");
                    out.println("</div>");
                    out.println("</div>");
                    out.println("</body>");
                    out.println("</html>");
                    
                    System.out.println("Methode executee : " + mapping.className + "." + mapping.method.getName());
                } else {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().println("La methode n'a pas retourne de String");
                    System.out.println("La methode ne retourne pas de String");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Erreur</title></head><body>");
                out.println("<h1>Erreur lors de l'execution de la methode</h1>");
                out.println("<p>" + e.getMessage() + "</p>");
                out.println("</body></html>");
                System.out.println("Erreur : " + e.getMessage());
            }
        } else {
            // Afficher la page 404 avec la liste des URLs disponibles
            show404Page(request, response, requestedUrl);
        }
    }

    private void show404Page(HttpServletRequest request, HttpServletResponse response, String requestedUrl) throws IOException {
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
        out.println("<h1>404 - Route non trouvee</h1>");
        out.println("<div class='error'>");
        out.println("<strong>URL demandee :</strong> " + requestedUrl);
        out.println("</div>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
        
        System.out.println("Route non trouvee : " + requestedUrl);
    }

    private void scanPackage(String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            
            for (File directory : dirs) {
                findClasses(directory, packageName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findClasses(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findClasses(file, packageName + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + 
                                     file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        analyzeClass(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void analyzeClass(Class<?> clazz) {
        // Verifier si la classe a l'annotation @AnnotationClasse
        if (clazz.isAnnotationPresent(AnnotationClasse.class)) {
            AnnotationClasse annClasse = clazz.getAnnotation(AnnotationClasse.class);
            String baseUrl = annClasse.value();
            
            System.out.println("\n Analyse de la classe : " + clazz.getSimpleName());
            System.out.println("  URL de base : " + baseUrl);
            
            // Parcourir toutes les methodes de la classe
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnnotationMethode.class)) {
                    AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
                    String methodUrl = annMethode.value();
                    
                    // Verifier que la methode retourne bien une String
                    if (method.getReturnType().equals(String.class)) {
                        // Construire l'URL complète
                        String fullUrl = baseUrl + methodUrl;
                        
                        // Creer le mapping
                        MappingInfo mappingInfo = new MappingInfo();
                        mappingInfo.controllerClass = clazz;
                        mappingInfo.className = clazz.getSimpleName();
                        mappingInfo.method = method;
                        
                        urlMappings.put(fullUrl, mappingInfo);
                        
                        System.out.println("    Methode mappee : " + method.getName() + "() → " + fullUrl);
                    } else {
                        System.out.println("    Methode ignoree (ne retourne pas String) : " + method.getName() + "()");
                    }
                }
            }
        }
        
        // Scanner aussi les classes sans @AnnotationClasse mais avec des methodes annotees
        else {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnnotationMethode.class)) {
                    AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
                    String methodUrl = annMethode.value();
                    
                    if (method.getReturnType().equals(String.class)) {
                        MappingInfo mappingInfo = new MappingInfo();
                        mappingInfo.controllerClass = clazz;
                        mappingInfo.className = clazz.getSimpleName();
                        mappingInfo.method = method;
                        
                        urlMappings.put(methodUrl, mappingInfo);
                        
                        if (urlMappings.size() == 1 || !clazz.getSimpleName().equals("")) {
                            System.out.println("\n Analyse de la classe : " + clazz.getSimpleName());
                        }
                        System.out.println("  Methode mappee : " + method.getName() + "() → " + methodUrl);
                    }
                }
            }
        }
    }

    // Classe pour stocker les informations de mapping
    private static class MappingInfo {
        Class<?> controllerClass;
        String className;
        Method method;
    }
}