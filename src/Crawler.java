//20140916
//PL
//SCUT Samsung Innovative Laboratory

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//---Crawler��ģ��---
//---���ã��������������ģ�飬���𴫴����������ģ�鲢��������ģ�顣---
public class Crawler {
	//---HttpClient�����߳���������URL����ȡ��URL������HTMLҳ��---	
	private int MAX_HTTPCLIENT;
	
	//---HtmlParser��������HTML�ļ���ȡURL�������---
	private int MAX_HTMLPARSER;
	
	//---HttpClientģ��---
	private HttpClient myHttpClient;
	
	//---HtmlParserģ��---
	private HtmlParser myHtmlParser; 
	
	//---��ץȡURL����---
	private LinkedBlockingQueue< String > urlQueue = new LinkedBlockingQueue< String >();
	
	//---������HTML�ļ�������---
	private LinkedBlockingQueue< String > htmlQueue = new LinkedBlockingQueue< String >();
	
	//---��־---
	private Logger logger;
	private static PrintWriter pw = null;
	
	//---����URL---
	public static String[] seedURL;
	
	//---�޶���ȡ��վ---
	public static String[] specifiedDomain = {
		"peopledaily", 
		"chinadaily", 
		"shanghaidaily", 
		"lifeofguangzhou", 
		"globaltimes", 
	};
	
	//---����״̬���---
	public static final int RUNNING = 0;
	public static final int PAUSING = 1;
	public static final int STOPPED = 2;
	public static final int REWINGING = 3;
	public int flag = PAUSING;
	
	//---constructor---
	public Crawler( int MAX_HTTPCLIENT, int MAX_HTMLPARSER ){
		this.MAX_HTTPCLIENT = MAX_HTTPCLIENT;
		this.MAX_HTMLPARSER = MAX_HTMLPARSER;
		
		//---����HttpClinetģ��---
		myHttpClient = new HttpClient( this, MAX_HTTPCLIENT, urlQueue, htmlQueue, logger );

		//---����HtmlParserģ��---
		myHtmlParser = new HtmlParser( this, MAX_HTMLPARSER, urlQueue, htmlQueue, specifiedDomain, logger );
	}
	
	//---��ʼ����־---
	{
//		logger = Logger.getLogger( "log" );
//		logger.setLevel( Level.ALL );
//		try{
//			FileHandler fileHandler;
//			fileHandler = new FileHandler( MyAPI.getRootDir() + "/Crawler.log" );
//			fileHandler.setFormatter( new Formatter(){
//				public String format(LogRecord arg0) {
//					Date date = new Date();
//					return "[" + date.toString() + "]"
//							+ "[" + arg0.getLevel() + "]"
//							+ arg0.getMessage() + "\n";
//				}
//			});
//		} catch( SecurityException | IOException e ){
//			Crawler.log( e.toString() );
//		}
		
		File file = new File( MyAPI.getRootDir() + "/log.txt" );
		if( !file.exists() ) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Crawler.log( e.toString() );
			}
		}
		
		try {
			pw = new PrintWriter( file );
		} catch (FileNotFoundException e) {
			Crawler.log( e.toString() );
		}
	}
	
	public static void log( String str ){
		Date date = new Date();
		pw.println( "[" + date.toString() + "]" + str );
	}
	
	//---push seed URLs into queue---
	public void enqueueSeedUrls(){
		File file = new File( MyAPI.getRootDir() + "/SeedUrls.txt" );
		if( !file.exists() ){
			try {
				file.createNewFile();
			} catch (IOException e) {
				Crawler.log( e.toString() );
			}
		}
		BufferedReader br = null;
		String str;
		try {
			br = new BufferedReader( new FileReader( file ) );
			while( ( str = br.readLine() ) != null ){
				urlQueue.put( str );
				Crawler.log( "Add to seed URLs: " + str );
			}
		} catch (FileNotFoundException e) {
			Crawler.log( e.toString() );
		} catch (IOException e) {
			Crawler.log( e.toString() );
		} catch (InterruptedException e) {
			Crawler.log( e.toString() );
		} finally {
			if( br != null ){
				try {
					br.close();
				} catch (IOException e) {
					Crawler.log( e.toString() );
				}
				br = null;
			}
		}
	}
	
	//---����---
	public void work(){
		//---������URL�������---
		enqueueSeedUrls();
		
		//---����HttpClientģ��---
		myHttpClient.work();
		
		//---����HtmlParserģ��---
		myHtmlParser.work();
	}
	
	//---��ʼ---
	public void start(){
		myHttpClient.start();
		myHtmlParser.start();
		flag = RUNNING;
	}
	
	//---��ͣ---
	public void pause(){
		myHttpClient.pause();
		myHtmlParser.pause();
		flag = PAUSING;
	}
	
	//---ֹͣ---
	public void stop(){
		myHttpClient.stop();
		myHtmlParser.stop();
		flag = STOPPED;
		if( pw != null ){
			pw.close();
			pw = null;
		}
	}
	
	//---������ȡ---
	public void rewind(){
		urlQueue.clear();
		htmlQueue.clear();
		enqueueSeedUrls();
		myHttpClient.filter.clear();
		myHtmlParser.filter.clear();
	}
}
