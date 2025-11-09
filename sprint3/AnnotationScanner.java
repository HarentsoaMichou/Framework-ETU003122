package sprint3;

import sprint2bis.AnnotationClasse;
import sprint2.AnnotationMethode;
import org.reflections.Reflections;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationScanner {

    private static final String BASE_PACKAGE = "testAnnotations";
    private static final Map<String, List<String>> annotatedMap = new HashMap<>();

    static {
        scanAnnotations();
    }

    private static void scanAnnotations() {
        Reflections reflections = new Reflections(BASE_PACKAGE);

        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);

        for (Class<?> clazz : allClasses) {
            List<String> annotatedMethods = new ArrayList<>();

            // Vérifie si la classe a l'annotation
            if (clazz.isAnnotationPresent(AnnotationClasse.class)) {
                annotatedMethods.add("[Classe annotée]");
            }

            // Vérifie les méthodes
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnnotationMethode.class)) {
                    annotatedMethods.add(method.getName());
                }
            }

            if (!annotatedMethods.isEmpty()) {
                annotatedMap.put(clazz.getSimpleName(), annotatedMethods);
            }
        }
    }

    public static Map<String, List<String>> getAnnotatedClasses() {
        return annotatedMap;
    }
}
