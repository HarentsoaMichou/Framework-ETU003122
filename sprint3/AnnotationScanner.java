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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AnnotationScanner extends HttpServlet {

    private List<ClassInfo> scannedClasses = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            // Scanner le package où se trouvent vos classes de test
            scanPackage("annotations");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Scan des Annotations</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1> Resultat du Scan des Annotations</h1>");
        out.println("<p>Nombre de classes scannees : <strong>" + scannedClasses.size() + "</strong></p>");
        
        for (ClassInfo info : scannedClasses) {
            out.println("<div class='class-box'>");
            out.println("<div class='class-name'>" + info.className + "</div>");
            
            if (info.hasClassAnnotation) {
                out.println("<div class='annotation'>");
                out.println("<span class='yes'>@AnnotationClasse</span> : \"" + info.classAnnotationValue + "\"");
                out.println("</div>");
            } else {
                out.println("<div class='annotation'> <span class='no'>Pas d'annotation de classe</span></div>");
            }
            
            if (!info.annotatedMethods.isEmpty()) {
                out.println("<div class='annotation'>");
                out.println("<strong>Methodes annotees :</strong>");
                for (MethodInfo method : info.annotatedMethods) {
                    out.println("<div class='method'>→ " + method.methodName + " : \"" + method.value + "\"</div>");
                }
                out.println("</div>");
            } else {
                out.println("<div class='annotation'><span class='no'>Pas de methode annotee</span></div>");
            }
            
            out.println("</div>");
        }
        
        out.println("</body>");
        out.println("</html>");
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
        ClassInfo info = new ClassInfo();
        info.className = clazz.getSimpleName();
        
        System.out.println("Classe: " + clazz.getSimpleName());
        
        // Vérifier l'annotation de classe
        if (clazz.isAnnotationPresent(AnnotationClasse.class)) {
            AnnotationClasse annClasse = clazz.getAnnotation(AnnotationClasse.class);
            info.hasClassAnnotation = true;
            info.classAnnotationValue = annClasse.value();
            System.out.println(" Annotation classe: Oui : Valeur: \"" + annClasse.value() + "\"");
        } else {
            System.out.println(" Annotation classe: Non");
        }
        
        // Vérifier les méthodes annotées
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AnnotationMethode.class)) {
                AnnotationMethode annMethode = method.getAnnotation(AnnotationMethode.class);
                MethodInfo methodInfo = new MethodInfo();
                methodInfo.methodName = method.getName();
                methodInfo.value = annMethode.value();
                info.annotatedMethods.add(methodInfo);
                System.out.println("Methode annotee: " + method.getName() + 
                                 "Valeur: \"" + annMethode.value() + "\"");
            }
        }
        
        if (info.annotatedMethods.isEmpty()) {
            System.out.println("Methodes annotees: Aucune");
        }
        
        System.out.println();
        scannedClasses.add(info);
    }

    // Classes internes pour stocker les informations
    private static class ClassInfo {
        String className;
        boolean hasClassAnnotation = false;
        String classAnnotationValue = "";
        List<MethodInfo> annotatedMethods = new ArrayList<>();
    }

    private static class MethodInfo {
        String methodName;
        String value;
    }
}