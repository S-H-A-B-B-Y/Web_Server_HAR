package HarServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/statusCheck")
public class StatusCheckServlet extends HttpServlet {

	public static boolean state=false;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");

        System.out.println("server State main "+state);
        if(action !=null)
        {
        	if(action.equals("stop"))
        	{
        		state=false;
        	}
        	else {
        		state=true;
        	}
        }
        else
        {
        	 if (state) {
                 response.setStatus(HttpServletResponse.SC_OK); // 200 OK
                 // Send the server status as a response to the Android app
                 response.getWriter().write("Server running");
                 //return;
             }
             else {
                 response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // 503 Service Unavailable
                 // Send the server status as a response to the Android app
                 response.getWriter().write("Server not running");
                 //return;
             }
        }

    }
}
