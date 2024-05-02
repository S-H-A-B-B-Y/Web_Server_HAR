package HarServer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/statusCheck")
public class StatusCheckServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static boolean state=false;
	public static String activityName="";
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");

        System.out.println("server State main "+state);

        if(action!=null)
        {
	        if(action.equals("setActivity"))
	    	{
	    		activityName=request.getParameter("selectedActivity");
	    		ServletContext context = getServletContext();
	
	             // Check if the attribute already exists
	            String currentActivity = (String) context.getAttribute("activity");
	             
	            if (currentActivity != null) {
	                 // If the attribute already exists, update its value
	                System.out.println("Activity updated to: "+ (String) context.getAttribute("activity"));
	
	                 context.setAttribute("activity", activityName);
	                 System.out.println("Activity updated to: "+ activityName);
	             } else {
	                 // If the attribute doesn't exist, initialize it with the new activity name
	                 context.setAttribute("activity", activityName);
	                 System.out.println("Activity Set to: "+ activityName);
	             }
	    	}
	    	else if(action.equals("start"))
	    	{
	    		state=true;
	    	}
	    	else if(action.equals("stop"))
	    	{
	    		state=false;
	    	}
        }
        else {
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
