package HarServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/updateServerStatus")
public class UpdateStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get the status from the request
        String status = request.getParameter("status");
        
        //System.out.println("Existing Status: " + status);
        // Update the server status in the session
        HttpSession session = request.getSession();
        session.setAttribute("serverStatus", status);

        // Send a success response
        response.getWriter().write("Status updated successfully");
    }
}
