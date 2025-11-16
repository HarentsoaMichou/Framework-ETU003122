package framework.service;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletContext;

public class ScanController {
    
    public ScanController() {}

    public static <T extends Annotation> List<Class<?>> findAllClassesWithAnnotation(
            ServletContext servletContext, Class<T> annotationClass) {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            String classesPath = servletContext.getRealPath("/WEB-INF/classes");
            
            if (classesPath == null) {
                System.err.println(" WEB-INF/classes introuvable");
                return classes;
            }
            
            File classesDir = new File(classesPath);
            
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                System.err.println(" Le repertoire WEB-INF/classes n'existe pas");
                return classes;
            }
            
            System.out.println("Scan du repertoire : " + classesPath);
            scanDirectoryForAnnotation(classesDir, "", classes, annotationClass);
            
        } catch (Exception e) {
            System.err.println(" Erreur lors du scan :");
            e.printStackTrace();
        }
        
        return classes;
    }

    public static List<Class<?>> findAllClasses(ServletContext servletContext) {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            String classesPath = servletContext.getRealPath("/WEB-INF/classes");
            
            if (classesPath == null) {
                return classes;
            }
            
            File classesDir = new File(classesPath);
            
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                return classes;
            }
            
            scanAllClasses(classesDir, "", classes);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return classes;
    }

    private static <T extends Annotation> void scanDirectoryForAnnotation(
            File directory, String packageName, List<Class<?>> classes, Class<T> annotationClass) {
        
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packageName.isEmpty() 
                    ? file.getName() 
                    : packageName + "." + file.getName();
                    
                scanDirectoryForAnnotation(file, newPackage, classes, annotationClass);
                
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                
                try {
                    Class<?> clazz = Class.forName(className);
                    
                    if (clazz.isAnnotationPresent(annotationClass)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Ignorer les classes non trouvees
                } catch (Exception e) {
                    // Ignorer les autres erreurs
                }
            }
        }
    }

    private static void scanAllClasses(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packageName.isEmpty() 
                    ? file.getName() 
                    : packageName + "." + file.getName();
                    
                scanAllClasses(file, newPackage, classes);
                
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Ignorer
                } catch (Exception e) {
                    // Ignorer
                }
            }
        }
    }
}