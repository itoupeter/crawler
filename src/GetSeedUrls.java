import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class GetSeedUrls extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		File file = new File( MyAPI.getRootDir() + "/SeedUrls.txt" );
		
		if( !file.exists() ){
			file.createNewFile();
		}
		
		BufferedReader br = new BufferedReader( new FileReader( file ) );
		JSONArray list = new JSONArray();
		JSONObject json = new JSONObject();
		String tmp;
		while( ( tmp = br.readLine() ) != null ){
			list.add( tmp );
		}
		json.element( "urls", list );
		
		PrintWriter pw = resp.getWriter();
		pw.println( json.toString() );
	}
}
