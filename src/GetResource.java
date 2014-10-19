import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

public class GetResource extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final int nResources = 5;
	
	//---store resource info: url, title, and body---
	private JSONObject[] resource = new JSONObject[ nResources ];

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		File file;
		BufferedReader br = null;
		String tmp;
		StringBuffer buffer = new StringBuffer( "" );
		
		file = new File( "/html1" );
		file.mkdir();
		
		for( int i = 0; i < nResources; ++i ){
			if( resource[ i ] == null ){
				resource[ i ] = new JSONObject();
			} else {
				resource[ i ].clear();
			}
			
			//---open cached html file---
			file = new File( "E:\\apache-tomcat-8.0.14\\webapps\\crawler\\WEB-INF\\html\\" + ( i + 1 ) + ".html" );
			if( !file.exists() ){
				resource[ i ].put( "url", "none" );
				resource[ i ].put( "title", "none" );
				resource[ i ].put( "abstract", "none" );
				continue;
			}
			
			br = new BufferedReader( new FileReader( file ) );
			//---absorb <!--- line---
			br.readLine();
			//---absorb [url] line---
			br.readLine();
			//---get url---
			resource[ i ].put( "url", br.readLine() );
			//---absorb [title] line---
			br.readLine();
			//---get title---
			resource[ i ].put( "title", br.readLine() );
			//---absorb [body] line---
			br.readLine();
			//---get body---
			while( ( tmp = br.readLine() ).indexOf( "--->" ) == -1 ){
				buffer.append( tmp );
			}
			if( buffer.length() > 0 ){
				tmp = buffer.substring( 0, 128 < buffer.length() ? 128 : buffer.length() );
			} else {
				tmp = "none";
			}
			resource[ i ].put( "abstract", tmp );
			//---close buffered reader---
			br.close();
			br = null;
		}

		JSONObject json = new JSONObject();
		json.put( "total", nResources );
		json.put( "results", resource );
		
		PrintWriter pw = resp.getWriter();
		pw.print( json.toString() );
	}

}
