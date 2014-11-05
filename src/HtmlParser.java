//20140917
//PL
//SCUT Samsung Innovative Laboratory

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


//---HtmlParserģ��---
//---���ã����ϴ�htmlQueue������ȡ��HTML�ļ������򿪸��ļ�����ȡ���Ϸ���URL�����غ����urlQueue�����С�---
public class HtmlParser {
	//---Crawler---
	private Crawler crawler;
	
	//---��־����---
	public Logger logger;
	
	//---parser����---
	private int nParsers;
	
	//---parser����---
	private Parser parsers[];
	
	//---��������HTML�ļ�������---
	private LinkedBlockingQueue< String > htmlQueue;
	
	//---��ץȡURL����---
	private LinkedBlockingQueue< String > urlQueue;
	
	//---����״̬���---
	private static final int RUNNING = 0;
	private static final int PAUSING = 1;
	private static final int STOPPED = 2;
	private static final int REWINDING = 3;
	private int flag = PAUSING;
	
	//---URL����ģ��---
	public BloomFilter filter = new BloomFilter();
	
	//---constructor---
	//---nParsers: ͬʱ������Parser������---
	//---htmlQueue: �����������HTML�ļ����ļ����Ķ���---
	//---urlQueue: �������ȡ��URL�Ķ���---
	//---logger: ��־����---
	public HtmlParser( Crawler crawler, int nParsers, LinkedBlockingQueue< String > urlQueue, LinkedBlockingQueue< String > htmlQueue, Logger logger ){
		this.crawler = crawler;
		this.nParsers = nParsers;
		this.htmlQueue = htmlQueue;
		this.urlQueue = urlQueue;
		this.logger = logger;
		
		//---��������nParsers��parser�Ķ���---
		parsers = new Parser[ nParsers ];
		for( int i = 0; i < nParsers; ++i ){
			parsers[ i ] = new Parser( i );
		}
	}
	
	//---������Parser---
	public void work(){
		for( int i = 0; i < nParsers; ++i ){
			parsers[ i ].start();
		}
	}
	
	//---ֹͣParser---
	public void stop(){
		flag = STOPPED;
	}
	
	//---��ͣParser---
	public void pause(){
		flag = PAUSING;
	}
	
	//---��ʼParser---
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
		private int nUrls;
		private int id;
		
		public Parser( int id ){
			this.id = id;
		}
		
		@Override
		public void run() {
			while ( true ) {
				//---�ж�����״̬---
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
				
				//---��֤HTML������URL���г����൱---
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
				
				//---��ȡHTML�ļ���---
				try {
					filename = htmlQueue.poll( 1, TimeUnit.SECONDS );
				} catch (InterruptedException e) {
					e.printStackTrace();
					//---CODE2003---
					logger.warning( "CODE2003" );
				}
				
				//---��filenameΪnull�������Ϊ�գ�������һ���ڡ�---
				if( filename == null ) continue;
				
				//---��HTML�ļ���ȡ���ݵ�html�ַ���---
				file = new File( MyAPI.getRootDir() + "/html/" + filename );
				buffer = new StringBuffer( "" );
				try {
					br = new BufferedReader( new FileReader( file ) );
					//---����<!---����[URL]��---
					br.readLine();
					br.readLine();
					//---��ȡ��HTML�ļ�����ԴURL---
					url = br.readLine();
					//---������--->��---
					while( br.readLine().indexOf( "--->" ) == -1 );
					//---��ȡHTML�ļ�������---
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
				
				//---��html�ַ���������URL---
				logger.info( "Parser_" + id + " Parsing: " + url );
				urlList = getAnchorTagUrls( url, html );
				nUrls = urlList.size();
				
				for( int i = 0; i < nUrls; ++i ){
					url = urlList.get( i );
					
					//---��ȡ�޶���վ�ڵ�URL---
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
					
					//---URL����---
					if( filter.isUrlChecked( url ) ) continue;
					
					//---����������URL����urlQueue---
					try {
						urlQueue.put( url );
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE2007---
						logger.warning( "CODE2007" );
					}
//					System.out.println( "Parser_" + id + " ������У�" + url );
				}
//				System.out.println( "Parser_" + id + " �������." );
			}
		}
		
		//---��HTML�ļ�����������URL---
		public ArrayList< String > getAnchorTagUrls( String url, String html ){
			//---û��HTML����---
			if ( html == null) return null;
			
			ArrayList< String > list = new ArrayList<String>();
			int head = 0;
			int tail = 0;
			while ( head != -1 ) {
				//---������һ��<a>��ǩͷ����λ��---
				head = html.toLowerCase().indexOf( "<a ", head );
				
				//---û���ҵ�<a>��ǩ����ֹ����---
				if( head == -1 ) break;
				
				//---�ҵ�<a>��ǩͷ���������Ӧβ��---
				tail = html.indexOf( ">", head );
				
				//---��������ʽ��URL---
				//---�ѵȺţ�=�����ҵĿո�ȥ��---
				String str;
				str = html.substring( head, tail == -1 ? html.length() : tail );
				str = str.replaceAll( "\\s*=\\s*", "=" );
				
				//---��ȡhref=�������---
				if( str.toLowerCase().matches( "^<a.*href\\s*=\\s*[\'|\"]?.*" ) ){
					int hrefIndex = str.toLowerCase().indexOf( "href=" );
					int leadingQuotesIndex = -1;
					
					//---����[href="www.baidu.com"]������---
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
					
					//---����[href='www.baidu.com']������---
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
					
					//---����[href= www.baidu.com ]������---
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
		
		//---������ַURL����link---
		public String urlHandler1( String url, String link ){
			if ( link == null ) return null;
			
			//---ȥ����β�ո�---
			link = link.trim();

			//---link������������URL---
			if ( link.toLowerCase().startsWith( "http://" ) || link.toLowerCase().startsWith( "https://" ) ) return link;
			
			//---link��������URL---
			String pare = url.trim();
			
			//---�������·��---
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

		//---��һ������URL---
		public String urlHandler2( String str ){
			//---����URL�е�/../��/./---
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
			
			//---ȥ��#��---
			int index2 = str.indexOf( '#' );
			if ( index2 != -1 ) str = str.substring( 0, index2 );
			
			//---�ų�һЩ����������ʽ������ַ������---
			if ( str.indexOf( '\'' ) != -1 ) str = "";
			if ( str.indexOf( "javascript") != -1 ) str = "";
			if ( str.indexOf( "js" ) != -1 ) str = "";
			if ( str.indexOf( "mailto:" ) != -1 ) str = "";

			//---����url�����Է�ֹspider trap---
			if ( str.length() > 200 )
				str = "";

			return str;
		}
	}
}
