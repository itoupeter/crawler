//20140916
//PL
//SCUT Samsung Innovative Laboratory

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
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
	
	//---������ҳ����---
	private int MAX_HTML;
	
	//---���Ͷ��д�С---
	private int RESOURCE_BUFFER_SIZE;
	
	//---HttpClientģ��---
	public HttpClient myHttpClient;
	
	//---HtmlParserģ��---
	public HtmlParser myHtmlParser; 
	
	//---��ץȡURL����---
	private LinkedBlockingQueue< String > urlQueue = new LinkedBlockingQueue< String >();
	
	//---������HTML�ļ�������---
	private LinkedBlockingQueue< String > htmlQueue = new LinkedBlockingQueue< String >();
	
	//---��־---
	public Logger logger;
	private FileHandler fileHandler;
	
	//---����URL---
	public static String[] seedURL;
	
	//---�޶���ȡ��վ---
	public static String[] specifiedDomain;
	
	//---����״̬���---
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
				//---CODE1000---
				logger.warning( "CODE1000" );
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
			//---CODE1001---
			logger.warning( "CODE1001" );
		} catch (NumberFormatException e) {
			e.printStackTrace();
			//---CODE1002---
			logger.warning( "CODE1002" );
		} catch (IOException e) {
			e.printStackTrace();
			//---CODE1003---
			logger.warning( "CODE1003" );
		}
		
		//---����HttpClinetģ��---
		myHttpClient = new HttpClient( this, MAX_HTTPCLIENT, urlQueue, htmlQueue, MAX_HTML, RESOURCE_BUFFER_SIZE, logger );

		//---����HtmlParserģ��---
		myHtmlParser = new HtmlParser( this, MAX_HTMLPARSER, urlQueue, htmlQueue, logger );
	}
	
	//---��ʼ����־---
	{
		logger = Logger.getLogger( "log" );
		logger.setLevel( Level.ALL );
		try{
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
			String logFileName = MyAPI.getRootDir() + "/log/log-" + sdf.format( new Date() ) + ".log";
			fileHandler = new FileHandler( logFileName );
			fileHandler.setFormatter( new Formatter(){
				public String format(LogRecord arg0) {
					Date date = new Date();
					return "[" + date.toString() + "]"
							+ "[" + arg0.getLevel() + "]"
							+ arg0.getMessage() + "\n";
				}
			});
			logger.addHandler( fileHandler );
		} catch( SecurityException e ){
			e.printStackTrace();
		} catch( IOException e ){
			e.printStackTrace();
		}
	}
	
	//---push seed URLs into queue---
	public void enqueueSeedUrls(){
		File file = new File( MyAPI.getRootDir() + "/SeedUrls.txt" );
		if( !file.exists() ){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				//---CODE1004---
				logger.warning( "CODE1004" );
			}
		}
		File file2 = new File( MyAPI.getRootDir() + "/Domains.txt" );
		if( !file2.exists() ){
			try{
				file.createNewFile();
			} catch ( IOException e ){
				e.printStackTrace();
				//---CODE1005---
				logger.warning( "CODE1005" );
			}
		}
		BufferedReader br = null;
		String str;
		try {
			br = new BufferedReader( new FileReader( file ) );
			while( ( str = br.readLine() ) != null ){
				urlQueue.put( str );
				logger.info( "Add to seed URLs: " + str );
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			//---CODE1006---
			logger.warning( "CODE1006" );
		} catch (IOException e) {
			e.printStackTrace();
			//---CODE1007---
			logger.warning( "CODE1007" );
		} catch (InterruptedException e) {
			e.printStackTrace();
			//---CODE1008---
			logger.warning( "CODE1008" );
		} finally {
			if( br != null ){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					//---CODE1009---
					logger.warning( "CODE1009" );
				}
				br = null;
			}
		}
		LinkedList< String > domainList = new LinkedList< String >();
		try{
			br = new BufferedReader( new FileReader( file2 ) );
			while( ( str = br.readLine() ) != null ){
				domainList.add( str );
			}
			specifiedDomain = new String[ domainList.size() ];
			for( int i = 0; i < specifiedDomain.length; ++i ){
				specifiedDomain[ i ] = domainList.getFirst();
				domainList.removeFirst();
			}
		} catch ( FileNotFoundException e ){
			e.printStackTrace();
			//---CODE1010---
			logger.warning( "CODE1010" );
		} catch ( IOException e ){
			e.printStackTrace();
			//---CODE1011---
			logger.warning( "CODE1011" );
		} finally {
			if( br != null ){
				try{
					br.close();
				} catch ( IOException e ){
					e.printStackTrace();
					//---CODE1012---
					logger.warning( "CODE1012" );
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
	}
	
	//---������ȡ---
	public void rewind(){
		try{
			logger.removeHandler( fileHandler );
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
			String logFileName = MyAPI.getRootDir() + "/log/log-" + sdf.format( new Date() ) + ".log";
			fileHandler = new FileHandler( logFileName );
			fileHandler.setFormatter( new Formatter(){
				public String format(LogRecord arg0) {
					Date date = new Date();
					return "[" + date.toString() + "]"
							+ "[" + arg0.getLevel() + "]"
							+ arg0.getMessage() + "\n";
				}
			});
			logger.addHandler( fileHandler );
		} catch( SecurityException e ){
			e.printStackTrace();
		} catch( IOException e ){
			e.printStackTrace();
		}
		urlQueue.clear();
		htmlQueue.clear();
		enqueueSeedUrls();
		myHttpClient.filter.clear();
		myHtmlParser.filter.clear();
	}
}
