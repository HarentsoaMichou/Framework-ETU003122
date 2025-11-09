package sprint2bis;

import sprint2bis.annotations.MonAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AnnotationScanner implements CommandLineRunner {

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(String... args) {

        String[] allBeans = context.getBeanDefinitionNames();
        for (String beanName : allBeans) {
            Object bean = context.getBean(beanName);
            Class<?> clazz = bean.getClass();

            if (clazz.isAnnotationPresent(MonAnnotation.class)) {
                MonAnnotation annotation = clazz.getAnnotation(MonAnnotation.class);
                System.out.println(clazz.getName() + " -> valeur : " + annotation.value());
            }
        }
    }
}
