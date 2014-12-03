import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class GetCrawlLog extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		int page = Integer.parseInt( req.getParameter( "page" ) );
		
		PrintWriter pw = resp.getWriter();
		if( CrawlerServlet.crawler == null ){
			pw.print( "[]" );
			return;
		}
		
		JSONArray list = new JSONArray();
		LinkedBlockingQueue< String > logQueue = CrawlerServlet.crawler.myHttpClient.logQueue;
		int count = 0;
		for( Iterator< String > ite = logQueue.iterator(); ite.hasNext(); ++count ){
			if( count / 20 < page ) continue;
			if( count / 20 > page ) break;
			JSONObject json = new JSONObject();
			json.put( "url", ite.next() );
			json.put( "passed", false );
			json.put( "message", "Can not download" );
			list.add( json );
		}
		pw.print( list.toString() );
	}
	
}
