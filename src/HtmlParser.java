//20140917
//PL
//SCUT Samsung Innovative Laboratory

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


//---HtmlParser模块---
//---作用：不断从htmlQueue队列中取出HTML文件名，打开该文件，提取出合法的URL，排重后加入urlQueue队列中。---
public class HtmlParser {
	//---Crawler---
	private Crawler crawler;
	
	//---日志工具---
	public Logger logger;
	
	//---parser数量---
	private int nParsers;
	
	//---parser队列---
	private Parser parsers[];
	
	//---待分析的HTML文件名队列---
	private LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > htmlQueue;
	
	//---待抓取URL队列---
	private LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > urlQueue;
	
	//---运行状态标记---
	private static final int RUNNING = 0;
	private static final int PAUSING = 1;
	private static final int STOPPED = 2;
	private static final int REWINDING = 3;
	private int flag = PAUSING;
	
	//---URL排重模块---
	public BloomFilter filter = new BloomFilter();
	
	//---constructor---
	//---nParsers: 同时工作的Parser的数量---
	//---htmlQueue: 储存待分析的HTML文件的文件名的队列---
	//---urlQueue: 储存待爬取的URL的队列---
	//---logger: 日志工具---
	public HtmlParser( Crawler crawler, int nParsers, LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > urlQueue, LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > htmlQueue, Logger logger ){
		this.crawler = crawler;
		this.nParsers = nParsers;
		this.htmlQueue = htmlQueue;
		this.urlQueue = urlQueue;
		this.logger = logger;
		
		//---创建含有nParsers个parser的队列---
		parsers = new Parser[ nParsers ];
		for( int i = 0; i < nParsers; ++i ){
			parsers[ i ] = new Parser( i );
		}
	}
	
	//---启动各Parser---
	public void work(){
		for( int i = 0; i < nParsers; ++i ){
			parsers[ i ].start();
		}
	}
	
	//---停止Parser---
	public void stop(){
		flag = STOPPED;
	}
	
	//---暂停Parser---
	public void pause(){
		flag = PAUSING;
	}
	
	//---开始Parser---
	public void start(){
		flag = RUNNING;
	}
	
	//---Parser---
	public class Parser extends Thread{
		private String url;
		private String tmp;
		private StringBuffer buffer;
		private String html;
		private String filename;
		private File file;
		private BufferedReader br;
		private ArrayList< String > urlList;
		private int depth;
		private int nUrls;
		private int id;
		
		public Parser( int id ){
			this.id = id;
		}
		
		@Override
		public void run() {
			while ( true ) {
				//---判断运行状态---
				if( flag == STOPPED ) {
					break;
				} else if( flag == PAUSING || flag == REWINDING ){
					try {
						Thread.sleep( 1000 );
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE2000---
						logger.warning( "CODE2000" );
					}
				} else if( flag == RUNNING ){
					try{
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ){
						e.printStackTrace();
						//---CODE2001---
						logger.warning( "CODE2001" );
					}
				}
				
				//---保证HTML队列与URL队列长度相当---
				if( urlQueue.size() > htmlQueue.size() ){
					try {
						Thread.sleep( 1000 );
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE2002---
						logger.warning( "CODE2002" );
					}
				}
				
				//---获取HTML文件名---
				AbstractMap.SimpleEntry< String, Integer > entry = null;
				try {
					entry = htmlQueue.poll( 1, TimeUnit.SECONDS );
				} catch (InterruptedException e) {
					e.printStackTrace();
					//---CODE2003---
					logger.warning( "CODE2003" );
				}
				
				//---若filename为null，则队列为空，跳到下一周期。---
				if( entry == null ) continue;
				filename = entry.getKey();
				depth = entry.getValue();
				
				//---从HTML文件读取内容到html字符串---
				file = new File( MyAPI.getRootDir() + "/html/" + filename );
				buffer = new StringBuffer( "" );
				try {
					br = new BufferedReader( new FileReader( file ) );
					//---吸收<!---行与[URL]行---
					br.readLine();
					br.readLine();
					//---获取该HTML文件的来源URL---
					url = br.readLine();
					//---吸收至--->行---
					while( br.readLine().indexOf( "--->" ) == -1 );
					//---获取HTML文件的内容---
					while( ( tmp = br.readLine() ) != null ) buffer.append( tmp ).append( "\n" );
					html = buffer.toString();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					//---CODE2004---
					logger.warning( "CODE2004" );
				} catch (IOException e) {
					e.printStackTrace();
					//---CODE2005---
					logger.warning( "CODE2005" );
				} finally {
					if( br != null ){
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
							//---CODE2006---
							logger.warning( "CODE2006" );
						}
						br = null;
					}
				}
				
				//---从html字符串分析出URL---
				logger.info( "Parser_" + id + " Parsing: " + url );
				urlList = getAnchorTagUrls( url, html );
				nUrls = urlList.size();
				
				for( int i = 0; i < nUrls; ++i ){
					url = urlList.get( i );
					
					//---爬取限定网站内的URL---
					if( Crawler.specifiedDomain.length > 0 ){
						boolean isSpecied = false;
						for( int j = 0; j < Crawler.specifiedDomain.length; ++j ){
							if( url.indexOf( Crawler.specifiedDomain[ j ] ) != -1 ) {
								isSpecied = true;
								break;
							}
						}
						if( !isSpecied ) continue;
					}
					
					//---URL排重---
					if( filter.isUrlChecked( url ) ) continue;
					
					//---将分析所得URL加入urlQueue---
					try {
						urlQueue.put( new AbstractMap.SimpleEntry<String, Integer>( url, depth + 1 ) );
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE2007---
						logger.warning( "CODE2007" );
					}
//					System.out.println( "Parser_" + id + " 加入队列：" + url );
				}
//				System.out.println( "Parser_" + id + " 分析完成." );
			}
		}
		
		//---从HTML文件分析出所有URL---
		public ArrayList< String > getAnchorTagUrls( String url, String html ){
			//---没有HTML内容---
			if ( html == null) return null;
			
			ArrayList< String > list = new ArrayList<String>();
			int head = 0;
			int tail = 0;
			while ( head != -1 ) {
				//---查找下一个<a>标签头部的位置---
				head = html.toLowerCase().indexOf( "<a ", head );
				
				//---没有找到<a>标签，终止分析---
				if( head == -1 ) break;
				
				//---找到<a>标签头部，找其对应尾部---
				tail = html.indexOf( ">", head );
				
				//---用正则表达式找URL---
				//---把等号（=）左右的空格去掉---
				String str;
				str = html.substring( head, tail == -1 ? html.length() : tail );
				str = str.replaceAll( "\\s*=\\s*", "=" );
				
				//---提取href=后的链接---
				if( str.toLowerCase().matches( "^<a.*href\\s*=\\s*[\'|\"]?.*" ) ){
					int hrefIndex = str.toLowerCase().indexOf( "href=" );
					int leadingQuotesIndex = -1;
					
					//---形如[href="www.baidu.com"]的链接---
					if( ( leadingQuotesIndex = str.indexOf( "\"", hrefIndex + "href=".length() ) ) != -1 ){
						int trailingQuotesIndex = str.indexOf( "\"", leadingQuotesIndex + 1 );
						
						if( trailingQuotesIndex == -1 ) trailingQuotesIndex = str.length();
						str = str.substring( leadingQuotesIndex + 1, trailingQuotesIndex );
						str = urlHandler1( url, str );
						str = urlHandler2( str );
						head += "<a ".length();
						if( str.equals( "" ) ) continue;
						list.add( str );
						continue;
					}
					
					//---形如[href='www.baidu.com']的链接---
					if( ( leadingQuotesIndex = str.indexOf( "\'", hrefIndex + "href=".length() ) ) != -1 ){
						int trailingQuotesIndex = str.indexOf( "\'", leadingQuotesIndex + 1 );

						if( trailingQuotesIndex == -1 ) trailingQuotesIndex = str.length();
						str = str.substring( leadingQuotesIndex + 1, trailingQuotesIndex );
						str = urlHandler1( url, str );
						str = urlHandler2( str );
						head += "<a ".length();
						if( str.equals( "" ) ) continue;
						list.add( str );
						continue;
					}
					
					//---形如[href= www.baidu.com ]的链接---
					int whitespaceIndex = str.indexOf( " ", hrefIndex + "href=".length() );
					
					if( whitespaceIndex == -1 ) whitespaceIndex = str.length();
					str = str.substring( hrefIndex + "href=".length(), whitespaceIndex );
					str = urlHandler1( url, str );
					str = urlHandler2( str );
					if( !str.equals( "" ) ) list.add( str );
				}
				head += "<a ".length();
			}
			return list;
		}
		
		//---根据网址URL处理link---
		public String urlHandler1( String url, String link ){
			if ( link == null ) return null;
			
			//---去除首尾空格---
			link = link.trim();

			//---link本身已是完整URL---
			if ( link.toLowerCase().startsWith( "http://" ) || link.toLowerCase().startsWith( "https://" ) ) return link;
			
			//---link不是完整URL---
			String pare = url.trim();
			
			//---处理相对路径---
			if ( !link.startsWith( "/" ) ) {
				if ( pare.endsWith( "/" ) ) return pare + link;
				if ( url.lastIndexOf( "/" ) == url.indexOf( "//" ) + 1
						|| url.lastIndexOf( "/" ) == url.indexOf( "//" ) + 2 ) {
					return pare + "/" + link;
				} else {
					return url.substring( 0, url.lastIndexOf( "/" ) + 1 ) + link;
				}
			} else {
				if ( url.lastIndexOf( "/" ) == url.indexOf( "//" ) + 1
						|| url.lastIndexOf( "/" ) == url.indexOf( "//" ) + 2 ) {
					return pare + link;
				} else {
					return url.substring( 0, url.indexOf( "/", url.indexOf( "//" ) + 3 ) ) + link;
				}
			}
		}

		//---进一步处理URL---
		public String urlHandler2( String str ){
			//---处理URL中的/../和/./---
			String[] tmp = str.split( "/", -1 );
			int len = tmp.length;
			
			for ( int i = 0; i < len; ++i ) {
				if ( tmp[ i ].equals( ".." ) ) {
					tmp[ i ] = "";
					int t = i;
					while ( tmp[ --t ] == "" );
					tmp[ t ] = "";
				} else if ( tmp[ i ].equals( "." ) ) {
					tmp[ i ] = "";
				}
			}
			str = "http://";
			for ( int i = 2; i < len; ++i ) {
				str += tmp[ i ];
				if ( i != len - 1 && !tmp[ i ].equals( "" ) )
					str += "/";
			}
			
			//---去除#号---
			int index2 = str.indexOf( '#' );
			if ( index2 != -1 ) str = str.substring( 0, index2 );
			
			//---排除一些符合正则表达式但非网址的链接---
			if ( str.indexOf( '\'' ) != -1 ) str = "";
			if ( str.indexOf( "javascript") != -1 ) str = "";
			if ( str.indexOf( "js" ) != -1 ) str = "";
			if ( str.indexOf( "mailto:" ) != -1 ) str = "";

			//---限制url长度以防止spider trap---
			if ( str.length() > 200 )
				str = "";

			return str;
		}
	}
}
