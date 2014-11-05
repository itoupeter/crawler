//20141012
//PL
//SCUT Samsung Innovative Laboratory


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrawlerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Crawler crawler = null;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter pw = resp.getWriter();
		pw.println( "Welcome to PL's Crawler!" );
		String state = "";
		if( crawler == null ){
			state = "STOPPED";
		} else {
			switch( crawler.flag ){
			case Crawler.PAUSING:
				state = "PAUSING";
				break;
			case Crawler.RUNNING:
				state = "RUNNING";
				break;
			default:
				state = "STOPPED";
				break;
			}
		}
		pw.println( "crawler state: " + state );
		
	}
}
