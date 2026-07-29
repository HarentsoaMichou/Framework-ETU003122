package framework.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    String value() default "";
    String method() default "GET"; // "GET" ou "POST"
}
