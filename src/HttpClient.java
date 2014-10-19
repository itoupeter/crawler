//20140917
//PL
//SCUT Samsung Innovative Laboratory

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;

//---HttpClientģ��---
//---���ã����ϴ�urlQueue������ȡ��URL�����ض�Ӧ��HTML�ļ������浽�����ļ��������ļ�������htmlQueue������---
public class HttpClient {
	//---Crawler---
	private Crawler crawler;
	
	//---��־---
	public Logger logger;
	
	//---DownloadThread����---
	private int nClients;
	
	//---DownloadThread����---
	private DownloadThread[] threads;
	
	//---HttpClient---
	private CloseableHttpClient httpClient;
	
	//---Client���ӹ���---
	private PoolingHttpClientConnectionManager connMgr;
	
	//---��������HTML�ļ�������---
	private LinkedBlockingQueue< String > htmlQueue;
	
	//---��ץȡURL����---
	private LinkedBlockingQueue< String > urlQueue;
	
	//---���ɱ���HTML�ļ���---
	private AtomicInteger filename = new AtomicInteger( 0 );
	
	//---HTML����ģ�飬��������---
	public BloomFilter filter = new BloomFilter();
	
	//---�����ȡ��ҳ����---
	private int MAX_HTML = 20;
	
	//---����״̬���---
	private static final int RUNNING = 0;
	private static final int PAUSING = 1;
	private static final int STOPPED = 2;
	private static final int REWINDING = 3;
	private int flag = PAUSING;
	
	//---��ʼ��httpClient��connMgr---
	{
		ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new DefaultConnectionKeepAliveStrategy(){
			@Override
			public long getKeepAliveDuration( HttpResponse response, HttpContext context ) {
				HeaderElementIterator iterator = new BasicHeaderElementIterator( response.headerIterator( HTTP.CONN_KEEP_ALIVE ) );
				
				while( iterator.hasNext() ){
					HeaderElement headerElement = iterator.nextElement();
		        	String name = headerElement.getName();
		        	String value = headerElement.getValue();
		        	if( value != null && name.equalsIgnoreCase( "timeout" ) ){
		        		try{
		        			return Long.parseLong( value ) * 1000;
		        		}catch( NumberFormatException e ){
		        			Crawler.log( e.toString() );
		        		}
		        	}
				}
				
				return 5 * 1000;
			}
			
		};
		
		connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setMaxTotal( 50 );
		connMgr.setDefaultMaxPerRoute( 2 );
		
		setHttpClient( HttpClients.custom()
				.setKeepAliveStrategy( connectionKeepAliveStrategy )
				.setConnectionManager( connMgr )
				.build() );
		
	}
	
	//---constructor---
	public HttpClient( Crawler crawler, int nClients, LinkedBlockingQueue< String > urlQueue, LinkedBlockingQueue< String > htmlQueue, Logger logger ){
		this.crawler = crawler;
		this.nClients = nClients;
		this.htmlQueue = htmlQueue;
		this.urlQueue = urlQueue;
		this.logger = logger;
		
		//---��������nClients�Ķ���---
		threads = new DownloadThread[ nClients ];
		for( int i = 0; i < nClients; ++i ){
			threads[ i ] = new DownloadThread( i );
		}
	}
	
	//---������client---
	public void work(){
		for( int i = 0; i < nClients; ++i ){
			threads[ i ].start();
		}
	}
	
	//---getter for httpClient---
	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	//---setter for httpClient---
	public void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	//---getter for connMgr---
	public PoolingHttpClientConnectionManager getConnMgr() {
		return connMgr;
	}

	//---setter for connMgr---
	public void setConnMgr( PoolingHttpClientConnectionManager connMgr ) {
		this.connMgr = connMgr;
	}
	
	//---ֹͣClient---
	public void stop(){
		flag = STOPPED;
	}
	
	//---��ͣClient---
	public void pause(){
		flag = PAUSING;
	}
	
	//---��ʼClient---
	public void start(){
		flag = RUNNING;
	}
	
	//---DownloadThread---
	public class DownloadThread extends Thread{
		private File file;
		private FileOutputStream fos = null;
		private PrintStream ps = null;
		private String url;
		private String html;
		private URL myURL;
		private URI myURI;
		private RequestConfig requestConfig = RequestConfig.custom()
        		.setConnectTimeout( 3000 )
        		.setSocketTimeout( 3000 )
        		.build();
		private int id;
		private TextDocument doc;
		private String body;
		
		public DownloadThread( int id ){
			this.id = id;
		}
		
		@Override
		public void run() {
			while( true ){
				//---�ж�����״̬---
				if( flag == STOPPED ) {
					break;
				} else if( flag == PAUSING || flag == REWINDING ) {
					try {
						Thread.sleep( 1000 );
						continue;
					} catch (InterruptedException e) {
						Crawler.log( e.toString() );
					}
				} else if( flag == RUNNING ){
					try{
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ){
						Crawler.log( e.toString() );
					}
				}

				//---��urlQueue��ȡURL---
				try {
					url = urlQueue.poll( 1, TimeUnit.SECONDS );
				} catch ( InterruptedException e ) {
					Crawler.log( e.toString() );
				}
				
				//---��urlΪnull�������Ϊ�գ�������һ����---
				if( url == null ) continue;
				
				//---����Ϊ�Ϸ�URI---
				try {
					myURL = new URL( url );
					myURI = new URI( myURL.getProtocol(), myURL.getHost(), myURL.getPath(), myURL.getQuery(), null );
				} catch ( MalformedURLException | URISyntaxException e ) {
					Crawler.log( e.toString() );
					continue;
				}
				
				//---����HTML����---
				HttpGet myHttpGet = new HttpGet( myURI );
				myHttpGet.addHeader( "User-Agent", "test_crawler" );
				myHttpGet.setConfig( requestConfig );
				try {
					Crawler.log( "Client_" + id + " Downloading: " + myURL.toString() );
					HttpResponse myHttpResponse = httpClient.execute( myHttpGet );
					Crawler.log( "Client_" + id + " Downloaded: " + myURL.toString() );
	                StatusLine statusLine = myHttpResponse.getStatusLine();
					HttpEntity entity = myHttpResponse.getEntity();

					//---HTML���ݹ��󣬲�����ҳ---
					if( entity.getContentLength() > 1000000 ){
//						System.out.println( "Client_" + id + " ����ʧ�ܣ�Content too large. Discarded." );
						continue;
					}

					//---����״̬����ʾ�쳣---
					if( statusLine.getStatusCode() >= 300 ){
						Crawler.log( "Client_" + id + " Download failed." );
						continue;
					}

					//---���ʵ��ַ�������HTML����---
					ContentType contentType = ContentType.getOrDefault( entity );
					Charset charset = contentType.getCharset();
					if( charset == null ) charset = HTTP.DEF_CONTENT_CHARSET;
					html = EntityUtils.toString( entity, charset );

					//---��boilerpipe��ȡ����---
					try {
						InputSource source = new HTMLDocument( html ).toInputSource();
						doc = new BoilerpipeSAXInput( source ).getTextDocument();
					} catch (BoilerpipeProcessingException e) {
						Crawler.log( e.toString() );
					} catch (SAXException e) {
						Crawler.log( e.toString() );
					}
					try {
						body = ArticleExtractor.INSTANCE.getText( doc );
					} catch (BoilerpipeProcessingException e) {
						Crawler.log( e.toString() );
					}

					//---�����غϣ�����---
					if( filter.isUrlChecked( body ) ) continue;

					//---����HTML������---
					file = new File( MyAPI.getRootDir() + "/html/" + filename.incrementAndGet() + ".html" );
					if( !file.exists() ){
						file.createNewFile();
					}
					fos = new FileOutputStream( file );
					ps = new PrintStream( fos );
					ps.println( "<!---\n[URL]\n" + myURL.toString() + "\n[title]\n" + doc.getTitle() + "\n[body]\n" + body + "\n--->" );
					ps.println( html );

					//---��HTML�ļ��������������---
					try {
						htmlQueue.put( file.getName() );
					} catch (InterruptedException e) {
						Crawler.log( e.toString() );
					}

					//---�ﵽץȡ��������ץȡ�����߳�0ִ��---
					if( id == 0 && filename.get() > MAX_HTML ){
						Crawler.log( "Crawler is being reset..." );
						crawler.pause();
						crawler.flag = Crawler.REWINGING;
						try {
							Thread.sleep( 5000 );
						} catch (InterruptedException e) {
							Crawler.log( e.toString() );
						}
						crawler.rewind();
						filename.set( 0 );
						if( crawler.flag == Crawler.REWINGING ) crawler.start();
						Crawler.log( "Crawler reset finished." );
					}
				} catch (IOException e) {
					Crawler.log( e.toString() );
				} finally{
					myHttpGet.releaseConnection();
					if( ps != null ){
						ps.close();
						ps = null;
					}
					if( fos != null ){
						try {
							fos.close();
						} catch (IOException e) {
							Crawler.log( e.toString() );
						}
						fos = null;
					}
				}
			}
		}
	}
}
