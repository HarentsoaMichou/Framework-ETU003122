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
        
        System.out.println(" Requete recue : " + path);
        
        // Verifier si c'est une URL mappee
        if (urlMappings.containsKey(path)) {
            handleMappedUrl(request, response, path);
            return;
        }
        
        // Verifier si c'est une ressource statique
        URL resource = getServletContext().getResource(path);
        if (resource != null) {
            RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
            if (defaultDispatcher != null) {
                defaultDispatcher.forward(request, response);
                return;
            }
        }
        
        // Afficher la page 404
        show404Page(response, path, request);
    }

    private void handleMappedUrl(HttpServletRequest request, HttpServletResponse response, String path) 
            throws ServletException, IOException {
        
        Mapping mapping = urlMappings.get(path);
        Method method = mapping.getMethod();
        
        try {
            // Creer une instance du controleur
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            
            // Invoquer la methode
            Object result = method.invoke(controllerInstance);
            
            // Traiter le resultat selon son type
            if (result instanceof String) {
                // Retour de type String : afficher directement
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Resultat</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<div class='container'>");
                out.println("<h1> Methode executee avec succès</h1>");
                out.println("<div class='info'>");
                out.println("<strong>URL :</strong> " + path + "<br>");
                out.println("<strong>Controleur :</strong> " + controllerClass.getSimpleName() + "<br>");
                out.println("<strong>Methode :</strong> " + method.getName() + "()");
                out.println("</div>");
                out.println("<div class='result'>");
                out.println(result);
                out.println("</div>");
                out.println("</div>");
                out.println("</body>");
                out.println("</html>");
                
                System.out.println(" [String] Methode executee : " + controllerClass.getSimpleName() + "." + method.getName());
                
            } else if (result instanceof ModelView) {
                // Retour de type ModelView : forward vers la vue JSP
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
                    response.getWriter().println("<p>Aucune vue specifiee dans ModelView !</p>");
                    response.getWriter().println("</body></html>");
                    
                    System.out.println(" ModelView sans vue definie");
                }
                
            } else {
                // Type de retour non supporte
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<!DOCTYPE html><html><body>");
                response.getWriter().println("<h1> Type de retour non supporte</h1>");
                response.getWriter().println("<p>La methode doit retourner String ou ModelView</p>");
                response.getWriter().println("<p>Type retourne : " + (result != null ? result.getClass().getName() : "null") + "</p>");
                response.getWriter().println("</body></html>");
                
                System.out.println(" Type de retour non supporte : " + (result != null ? result.getClass().getName() : "null"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Erreur</title></head>");
            out.println("<body>");
            out.println("<h1> Erreur lors de l'execution</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("<pre>" + e.getClass().getName() + "</pre>");
            out.println("</body></html>");
            
            System.out.println(" Erreur : " + e.getMessage());
        }
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