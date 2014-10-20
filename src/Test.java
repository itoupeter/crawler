//20140916
//PL
//SCUT Samsung Innovative Laboratory

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.sf.json.JSONObject;

public class Test {

	public static void main(String[] args) {
		new Test().run5();
	}

	// ---test Map.Entry---
	public void run() {
		Map.Entry<Integer, String> entry;

		entry = new AbstractMap.SimpleEntry<Integer, String>(new Integer(1), "abc");
		System.out.println("key: " + entry.getKey() + "\nval: "
				+ entry.getValue());
		System.out.println("hash: " + entry.hashCode());

		entry.setValue("bcd");
		System.out.println("\nkey: " + entry.getKey() + "\nval: "
				+ entry.getValue());
		System.out.println("hash: " + entry.hashCode());
	}

	//---test LinkedBlockingQueue---
	public void run2() {
		final LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue<Integer>();

		lbq.add(1);
		lbq.add(2);
		new Thread( new Runnable(){
			public void run() {
				try {
					Thread.sleep( 2000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		try {
			System.out.println(lbq.take());
			System.out.println(lbq.take());
			System.out.println(lbq.take());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	//---test multi-thread---
	public void run3(){
		System.out.println( "Main thread starts." );
		
		Thread child = new Thread( new Runnable(){
			public void run() {
				System.out.println( "Child thread starts." );
				try {
					Thread.sleep( 2000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println( "Child thread ends." );
			}
		});
		
		child.start();
		try {
			child.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println( "Main thread ends." );
	}
	
	//---test file writing---
	public void run4(){
		File file = new File( "html/1.html" );
		FileOutputStream fos = null;
		PrintStream ps = null;
		
		if( !file.exists() ){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			fos = new FileOutputStream( file );
			ps = new PrintStream( fos );
			ps.print( "<!---[URL]\nhttp://qq.com\n[HTML]--->\n<html><head>麻花藤</head><body>呵</body></html>" );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if( ps != null ){
				ps.close();
			}
			if( fos != null ){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//---test HttpClinet---
	public void run5(){
		Crawler crawler = new Crawler();
		crawler.work();
		crawler.start();
	}
	
	//---test regex---
	public void run6(){
		String str = "123 ???12??f?f??  ?";
		
		String str1 = str.replaceAll( "\\?+", " " );
		System.out.println( str1 );
		
		String str2 = str1.replaceAll( " +", " " );
		System.out.println( str2 );
	}
	
	//---test json---
	public void run7(){
		JSONObject jo = new JSONObject();
		jo.put( "total", 100 );
		
		JSONObject result1 = new JSONObject();
		result1.put( "title", "title1" );
		result1.put( "author", "author1" );
		
		JSONObject result2 = new JSONObject();
		result2.put( "title", "标题2" );
		result2.put( "author", "作者2" );
		
		jo.put( "results", new JSONObject[]{ result1, result2 } );
		System.out.println( jo.toString() );
	}
	
	//---client---
	public void run8(){
		Scanner scanner = new Scanner( System.in );
		int input;

		System.out.println( "0退出，1发送" );
		
		while( true ){
			input = scanner.nextInt();
			
			switch( input ){
			case 0:
				return;
			case 1:
				try {
					CloseableHttpClient client = HttpClients.createDefault();
					HttpGet get = new HttpGet( "http://localhost:8080/crawler/GetResource" );
					CloseableHttpResponse response = client.execute( get );
					HttpEntity entity = response.getEntity();
					System.out.print( EntityUtils.toString( entity ) );
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}
}
