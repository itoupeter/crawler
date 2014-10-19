import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;


public class MyAPI {

	public static String getRootDir(){
		File file = new File( "PLCrawler.txt" );
		String res = "";
		
		if( !file.exists() ){
			res = "../webapps/crawler";
			PrintWriter pw = null; 
			try {
				file.createNewFile();
				pw = new PrintWriter( file );
				pw.println( "[ApplicationRoot]" );
				pw.println( "../webapps/crawler" );
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if( pw != null ){
					pw.close();
					pw = null;
				}
			}
		} else {
			BufferedReader br = null;
			try {
				br = new BufferedReader( new FileReader( file ) );
				br.readLine();
				res = br.readLine();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if( br != null ){
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					br = null;
				}
			}
		}
		
		return res;
	}
}
