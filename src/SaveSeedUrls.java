import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class SaveSeedUrls extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost( req, resp );
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String data = req.getParameter( "data" );
		JSONObject json = JSONObject.fromObject( data );
		JSONArray list = json.getJSONArray( "urls" );
		File file = new File( MyAPI.getRootDir() + "/SeedUrls.txt" );
		if( !file.exists() ) file.createNewFile();
		
		PrintWriter pw = new PrintWriter( new FileWriter( file, true ) );
		for( Iterator iterator = list.iterator(); iterator.hasNext(); ){
			pw.println( iterator.next().toString() );
		}
		pw.close();
		
		json.clear();
		json.put( "passed", true );
		json.put( "message", "Save seed URLs succeeded." );
		pw = resp.getWriter();
		pw.println( json.toString() );
	}

}
