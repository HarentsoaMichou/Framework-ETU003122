package sprint1bis;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.RequestDispatcher;


public class FrontServletBis {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }


    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                boolean resourceExists = getServletContext().getResource(path) != null;

            if (resourceExists) {
                RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("default");
                dispatcher.forward(request, response);
            } else {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println("<h1>URL : " + path + "</h1>");
            }
    }
    
}
