//20141012
//PL
//SCUT Samsung Innovative Laboratory


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;


public class CrawlerAction extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String action = req.getParameter( "action" );
		PrintWriter pw = resp.getWriter();
		JSONObject json = new JSONObject();
		String msg = "";
		
		if( action.indexOf( "START" ) != -1 ){
			if( CrawlerServlet.crawler == null ){
				CrawlerServlet.crawler = new Crawler( 1, 1 );
				new Thread( new Runnable() {
					@Override
					public void run() {
						CrawlerServlet.crawler.work();
						CrawlerServlet.crawler.start();
					}
				}).start();
			} else {
				CrawlerServlet.crawler.start();
			}
			msg = "Crawler is working.";
		} else if( action.indexOf( "PAUSE" ) != -1 ){
			if( CrawlerServlet.crawler == null ){
				msg = "No crawler is working. Failed to pause.";
			} else {
				CrawlerServlet.crawler.pause();
				msg = "Crawler is paused.";
			}
		} else if( action.indexOf( "STOP" ) != -1 ){
			if( CrawlerServlet.crawler == null ){
				msg = "No crawler is working. Failed to stop.";
			} else {
				CrawlerServlet.crawler.stop();
				CrawlerServlet.crawler = null;
				msg = "Crawler is stopped.";
			}
		} else{
			json.put( "passed", false );
			json.put( "message", "Invalid action." );
			pw.println( json.toString() );
			return;
		}
		
		json.put( "passed", true );
		json.put( "message", msg );
		pw.println( json.toString() );
	}

}
