import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class RemoveSeedUrls extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JSONObject json = JSONObject.fromObject( req.getParameter( "data" ) );
		JSONArray list = json.getJSONArray( "urls" );
		File file = new File( MyAPI.getRootDir() + "/SeedUrls.txt" );
		File file2 = new File( MyAPI.getRootDir() + "/Domains.txt" );
		BufferedReader br = new BufferedReader( new FileReader( file ) );
		LinkedList< String > urls = new LinkedList<>();
		String tmp;
		while( ( tmp = br.readLine() ) != null ){
			urls.add( tmp );
		}
		br.close();
		PrintWriter pw = new PrintWriter( file );
		PrintWriter pw2 = new PrintWriter( file2 );
		for( Iterator<String> ite = urls.iterator(); ite.hasNext(); ){
			String str = ite.next();
			boolean flag = false;
			for( Iterator ite1 = list.iterator(); ite.hasNext(); ){
				String str1 = ite1.next().toString();
				if( str.equals( str1 ) ){
					flag = true;
					break;
				}
			}
			if( !flag ){
				pw.println( str );
				pw2.println( new URL( str ).getHost() );
			}
		}
		pw.close();
		pw2.close();
		pw = resp.getWriter();
		json = new JSONObject();
		json.put( "passed", true );
		json.put( "message", "Delete seed URL succeeded." );
		pw.print( json.toString() );
	}

}
