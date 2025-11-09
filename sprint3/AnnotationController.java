package sprint3;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.List;

@Controller
public class AnnotationController {

    @RequestMapping("/annotations")
    @ResponseBody
    public String showAnnotatedClasses() {
        StringBuilder sb = new StringBuilder("<h2>Classes et méthodes annotées détectées :</h2>");
        Map<String, List<String>> data = AnnotationScanner.getAnnotatedClasses();

        if (data.isEmpty()) {
            sb.append("<p>Aucune annotation détectée.</p>");
        } else {
            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                sb.append("<b>").append(entry.getKey()).append("</b><ul>");
                for (String m : entry.getValue()) {
                    sb.append("<li>").append(m).append("</li>");
                }
                sb.append("</ul>");
            }
        }
        return sb.toString();
    }
}
