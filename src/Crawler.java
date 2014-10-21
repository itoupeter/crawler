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

//---Crawler总模块---
//---作用：这是爬虫程序总模块，负责传达参数给各个模块并启动各个模块。---
public class Crawler {
	//---HttpClient下载线程数量，从URL队列取出URL并下载HTML页面---	
	private int MAX_HTTPCLIENT;
	
	//---HtmlParser数量，从HTML文件抽取URL加入队列---
	private int MAX_HTMLPARSER;
	
	//---下载网页数量---
	private int MAX_HTML;
	
	//---推送队列大小---
	private int RESOURCE_BUFFER_SIZE;
	
	//---HttpClient模块---
	private HttpClient myHttpClient;
	
	//---HtmlParser模块---
	private HtmlParser myHtmlParser; 
	
	//---待抓取URL队列---
	private LinkedBlockingQueue< String > urlQueue = new LinkedBlockingQueue< String >();
	
	//---待分析HTML文件名队列---
	private LinkedBlockingQueue< String > htmlQueue = new LinkedBlockingQueue< String >();
	
	//---日志---
	private Logger logger;
	private static PrintWriter pw = null;
	
	//---种子URL---
	public static String[] seedURL;
	
	//---限定爬取网站---
	public static String[] specifiedDomain = {
		"peopledaily", 
		"chinadaily", 
		"shanghaidaily", 
		"lifeofguangzhou", 
		"globaltimes", 
	};
	
	//---运行状态标记---
	public static final int RUNNING = 0;
	public static final int PAUSING = 1;
	public static final int STOPPED = 2;
	public static final int REWINGING = 3;
	public int flag = PAUSING;
	
	//---constructor---
	public Crawler(){
		File file = new File( MyAPI.getRootDir() + "/config.txt" );
		if( !file.exists() ){
			try {
				file.createNewFile();
				PrintWriter pw = new PrintWriter( file );
				pw.println( "[PARSER_THREAD]\n1\n[DOWNLOAD_THREAD]\n1\n[MAX_HTML]\n100" );
				pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedReader br = new BufferedReader( new FileReader( file ) );
			String tmp = "";
			while( ( tmp = br.readLine() ) != null ){
				if( tmp.indexOf( "PARSER_THREAD" ) != -1 ){
					tmp = br.readLine();
					MAX_HTMLPARSER = Integer.parseInt( tmp );
				} else if( tmp.indexOf( "DOWNLOAD_THREAD" ) != -1 ){
					tmp = br.readLine();
					MAX_HTTPCLIENT = Integer.parseInt( tmp );
				} else if( tmp.indexOf( "MAX_HTML" ) != -1 ){
					tmp = br.readLine();
					MAX_HTML = Integer.parseInt( tmp );
				} else if( tmp.indexOf( "RESOURCE_BUFFER_SIZE" ) != -1 ){
					tmp = br.readLine();
					RESOURCE_BUFFER_SIZE = Integer.parseInt( tmp );
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//---创建HttpClinet模块---
		myHttpClient = new HttpClient( this, MAX_HTTPCLIENT, urlQueue, htmlQueue, MAX_HTML, RESOURCE_BUFFER_SIZE, logger );

		//---创建HtmlParser模块---
		myHtmlParser = new HtmlParser( this, MAX_HTMLPARSER, urlQueue, htmlQueue, specifiedDomain, logger );
	}
	
	//---初始化日志---
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
	
	//---启动---
	public void work(){
		//---将种子URL加入队列---
		enqueueSeedUrls();
		
		//---启动HttpClient模块---
		myHttpClient.work();
		
		//---启动HtmlParser模块---
		myHtmlParser.work();
	}
	
	//---开始---
	public void start(){
		myHttpClient.start();
		myHtmlParser.start();
		flag = RUNNING;
	}
	
	//---暂停---
	public void pause(){
		myHttpClient.pause();
		myHtmlParser.pause();
		flag = PAUSING;
	}
	
	//---停止---
	public void stop(){
		myHttpClient.stop();
		myHtmlParser.stop();
		flag = STOPPED;
		if( pw != null ){
			pw.close();
			pw = null;
		}
	}
	
	//---重新爬取---
	public void rewind(){
		urlQueue.clear();
		htmlQueue.clear();
		enqueueSeedUrls();
		myHttpClient.filter.clear();
		myHtmlParser.filter.clear();
	}
}
