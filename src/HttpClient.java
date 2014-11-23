//20140917
//PL
//SCUT Samsung Innovative Laboratory

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.http.Consts;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
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

//---HttpClient模块---
//---作用：不断从urlQueue队列中取出URL，下载对应的HTML文件，储存到本地文件，并把文件名加入htmlQueue队列中---
public class HttpClient {
	//---Crawler---
	private Crawler crawler;
	
	//---日志---
	public Logger logger;
	
	//---DownloadThread数量---
	private int nClients;
	
	//---DownloadThread队列---
	private DownloadThread[] threads;
	
	//---HttpClient---
	private CloseableHttpClient httpClient;
	
	//---Client连接管理---
	private PoolingHttpClientConnectionManager connMgr;
	
	//---待分析的HTML文件名队列---
	private LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > htmlQueue;
	
	//---待抓取URL队列---
	private LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > urlQueue;
	
	//---生成本地HTML文件名---
	private AtomicInteger filename = new AtomicInteger( 0 );
	
	//---HTML排重模块，根据正文---
	public BloomFilter filter = new BloomFilter();
	
	//---最大爬取网页数量---
	private int MAX_HTML = 30;
	
	//---每次推送至solr服务器条目数---
	private int RESOURCE_BUFFER_SIZE;
	
	//---最大爬取深度---
	private int MAX_DEPTH;
	
	//---待推送资源队列---
	private LinkedBlockingQueue< Resource > resQueue;
	
	//---log queue---
	public LinkedBlockingQueue< String > logQueue;
	
	//---运行状态标记---
	private static final int RUNNING = 0;
	private static final int PAUSING = 1;
	private static final int STOPPED = 2;
	private static final int REWINDING = 3;
	private int flag = PAUSING;
	
	//---preceding noise amount---
	private int noise = 50;
	
	//---初始化httpClient和connMgr---
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
		        			e.printStackTrace();
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
	public HttpClient( Crawler crawler, int nClients, 
			LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > urlQueue, LinkedBlockingQueue< AbstractMap.SimpleEntry< String, Integer > > htmlQueue, 
			int MAX_HTML, int RESOURCE_BUFFER_SIZE, int MAX_DEPTH, Logger logger ){
		this.crawler = crawler;
		this.nClients = nClients;
		this.htmlQueue = htmlQueue;
		this.urlQueue = urlQueue;
		this.MAX_HTML = MAX_HTML;
		this.RESOURCE_BUFFER_SIZE = RESOURCE_BUFFER_SIZE;
		this.MAX_DEPTH = MAX_DEPTH;
		this.logger = logger;
		resQueue = new LinkedBlockingQueue<>();
		logQueue = new LinkedBlockingQueue<>();
		
		//---创建含有nClients的队列---
		threads = new DownloadThread[ nClients ];
		for( int i = 0; i < nClients; ++i ){
			threads[ i ] = new DownloadThread( i );
		}
	}
	
	//---启动各client---
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
	
	//---停止Client---
	public void stop(){
		flag = STOPPED;
	}
	
	//---暂停Client---
	public void pause(){
		flag = PAUSING;
	}
	
	//---开始Client---
	public void start(){
		flag = RUNNING;
	}
	
	//---DownloadThread---
	public class DownloadThread extends Thread{
		private RequestConfig requestConfig = RequestConfig.custom()
        		.setConnectTimeout( 3000 )
        		.setSocketTimeout( 3000 )
        		.build();
		private int id;
		
		public DownloadThread( int id ){
			this.id = id;
		}
		
		@Override
		public void run() {
			while( true ){
				//---判断运行状态---
				if( flag == STOPPED ) {
					break;
				} else if( flag == PAUSING || flag == REWINDING ) {
					try {
						Thread.sleep( 1000 );
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE3000---
						logger.warning( "CODE3000" );
					}
				} else if( flag == RUNNING ){
					try{
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ){
						e.printStackTrace();
						//---CODE3001---
						logger.warning( "CODE3001" );
					}
				}

				//---从urlQueue获取URL---
				AbstractMap.SimpleEntry< String, Integer > entry = null;
				String url = "";
				int depth = 0;
				try {
					entry = urlQueue.poll( 1, TimeUnit.SECONDS );
				} catch ( InterruptedException e ) {
					e.printStackTrace();
					//---CODE3002---
					logger.warning( "CODE3002" );
				}
				
				//---若url为null，则队列为空，跳到下一周期---
				if( entry == null ) continue;
				url = entry.getKey();
				depth = entry.getValue().intValue();
				
				//---处理为合法URI---
				URL myURL;
				URI myURI;
				try {
					myURL = new URL( url );
					myURI = new URI( myURL.getProtocol(), myURL.getHost(), myURL.getPath(), myURL.getQuery(), null );
				} catch ( MalformedURLException | URISyntaxException e ) {
					e.printStackTrace();
					//---CODE3003---
					logger.warning( "CODE3003" );
					continue;
				}
				
				//---下载HTML内容---
				HttpGet myHttpGet = new HttpGet( myURI );
				myHttpGet.addHeader( "User-Agent", "test_crawler" );
				myHttpGet.setConfig( requestConfig );
				CloseableHttpResponse myHttpResponse = null;
				StatusLine statusLine = null;
				HttpEntity entity = null;
				String html = null;
				ContentType contentType = ContentType.getOrDefault( entity );
				Charset charset = contentType.getCharset();
				if( charset == null ) charset = HTTP.DEF_CONTENT_CHARSET;
				try {
					logger.info( "Client_" + id + " Downloading: " + myURL.toString() );
					myHttpResponse = httpClient.execute( myHttpGet );
					logger.info( "Client_" + id + " Downloaded: " + myURL.toString() );
		            statusLine = myHttpResponse.getStatusLine();
					entity = myHttpResponse.getEntity();
					
					//---HTML内容过大，不是网页---
					if( entity.getContentLength() > 1000000 ){
						logger.warning( "Discard content from " + myURL.toString() );
						logQueue.add( myURL.toString() );
						continue;
					}

					//---返回状态码提示异常---
					if( statusLine.getStatusCode() >= 300 ){
						logger.warning( "Cannot download from " + myURL.toString() );
						logQueue.add( myURL.toString() );
						continue;
					}

					//---用适当字符集处理HTML内容---
					html = EntityUtils.toString( entity, charset );
				} catch( ClientProtocolException e ){
					//---错误协议，URI问题---
					e.printStackTrace();
					//---CODE3004---
					logger.warning( "CODE3004" );
					continue;
				} catch( IOException e ){
					//---连接网站失败---
					e.printStackTrace();
					//---CODE3005---
//					logger.warning( "CODE3005" );
					logger.warning( "Cannot download from " + myURL.toString() );
					logQueue.add( myURL.toString() );
					continue;
				} finally {
					if( myHttpResponse != null ){
						try {
							myHttpResponse.close();
						} catch (IOException e) {
							e.printStackTrace();
							//---CODE3006---
							logger.warning( "CODE3006" );
						}
					}
				}

				//---html为空---
				if( html == null ) continue;

				//---用boilerpipe提取正文---
				TextDocument doc;
				try {
					InputSource source = new HTMLDocument( html ).toInputSource();
					doc = new BoilerpipeSAXInput( source ).getTextDocument();
				} catch (BoilerpipeProcessingException e) {
					e.printStackTrace();
					//---CODE3007---
					logger.warning( "CODE3007" );
					continue;
				} catch (SAXException e) {
					e.printStackTrace();
					//---CODE3008---
					logger.warning( "CODE3008" );
					continue;
				}
				String body = "";
				String title = doc.getTitle();
				try {
					body = ArticleExtractor.INSTANCE.getText( doc );
					if( body == null || body.length() < 1 ) body = "";
					if( title == null || title.length() < 1 ) title = "";
				} catch (BoilerpipeProcessingException e) {
					//---CODE3009---
					logger.warning( "CODE3009" );
					e.printStackTrace();
					continue;
				}

				//---正文重合，丢弃---
				if( filter.isUrlChecked( body ) ) continue;

				//---缓存HTML到本地---
				File file = new File( MyAPI.getRootDir() + "/html/" + filename.incrementAndGet() + ".html" );
				if( !file.exists() ){
					try {
						file.createNewFile();
					} catch (IOException e) {
						//---failed to create file---
						e.printStackTrace();
						//---CODE3010---
						logger.warning( "CODE3010" );
						continue;
					}
				}
				PrintWriter pw = null;
				try {
					pw = new PrintWriter( file );
					pw.println( "<!---\n[URL]\n" + myURL.toString() + "\n[title]\n" + title + "\n[body]\n" + body + "\n--->" );
					pw.println( html );
				} catch (FileNotFoundException e2) {
					//---failed to write file---
					e2.printStackTrace();
					//---CODE3011---
					logger.warning( "CODE3011" );
					continue;
				} finally {
					if( pw != null ){
						pw.close();
						pw = null;
					}
				}
				
				//---将HTML文件加入待分析队列---
				if( depth < MAX_DEPTH ){
					try {
						htmlQueue.put( new AbstractMap.SimpleEntry< String, Integer >( file.getName(), depth ) );
					} catch (InterruptedException e) {
						//---failed to enqueue html---
						e.printStackTrace();
						//---CODE3012---
						logger.warning( "CODE3012" );
					}
				}

				//---加入待推送资源队列---
				if( noise > 0 ) {
					--noise;
				} else try{
					resQueue.put( 
							new Resource()
							.setUrl( myURL.toString() )
							.setTitle( title )
							.setBody( body )
							);
				} catch (InterruptedException e1) {
					//---failed to enqueue resource---
					e1.printStackTrace();
					//---CODE3013---
					logger.warning( "CODE3013" );
				}
					
				//---达到推送数量推送资源至solr服务器，由线程0执行---
				if( id == 0 && resQueue.size() >= RESOURCE_BUFFER_SIZE ){
					JSONArray list = new JSONArray();
					for( int i = 0; i < RESOURCE_BUFFER_SIZE; ++i ){
						try {
							Resource tmpRes = resQueue.take();
							JSONObject tmpJson = new JSONObject();
							tmpJson.put( "url", tmpRes.url );
							tmpJson.put( "title", tmpRes.title );
							tmpJson.put( "abstract", tmpRes.body );
							list.add( tmpJson );
						} catch (InterruptedException e) {
							e.printStackTrace();
							//---CODE3014---
							logger.warning( "CODE3014" );
						}
					}
					JSONObject json = new JSONObject();
					json.put( "total", RESOURCE_BUFFER_SIZE );
					json.put( "results", list );
					
					CloseableHttpClient httpClient = HttpClients.createDefault();
//					HttpPost httpPost = new HttpPost( "http://localhost:8080/crawler/GetResource" );
					HttpPost httpPost = new HttpPost( "http://localhost:8080/scut/SolrAdd" );
					List< NameValuePair > form = new ArrayList< NameValuePair >();
					form.add( new BasicNameValuePair( "data", json.toString() ) );
					UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity( form, Consts.UTF_8 );
					httpPost.setEntity( formEntity );
					CloseableHttpResponse response = null;
					try {
						response = httpClient.execute( httpPost );
//						HttpEntity respEntity = response.getEntity();
//						System.out.println( EntityUtils.toString( respEntity ) );
					} catch (ClientProtocolException e) {
						//---URI问题---
						e.printStackTrace();
						//---CODE3015---
						logger.warning( "CODE3015" );
					} catch (IOException e) {
						//---连接到solr服务器失败---
						e.printStackTrace();
						//---CODE3016---
						logger.warning( "CODE3016" );
					} finally{
						if( response != null ){
							try {
								response.close();
							} catch (IOException e) {
								e.printStackTrace();
								//---CODE3017---
								logger.warning( "CODE3017" );
							}
							response = null;
						}
					}
				}
				
				//---达到抓取数量重新抓取，由线程0执行---
				if( id == 0 && filename.get() > MAX_HTML ){
					logger.info( "Crawler is being reset..." );
					crawler.pause();
					crawler.flag = Crawler.REWINGING;
					try {
						Thread.sleep( 5000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
						//---CODE3018---
						logger.warning( "CODE3018" );
					}
					logQueue.clear();
					crawler.rewind();
					filename.set( 0 );
					if( crawler.flag == Crawler.REWINGING ) crawler.start();
					logger.info( "Crawler reset finished." );
				}
			}
		}
	}
	
	public class Resource{
		public String url;
		public String title;
		public String body;
		
		public Resource setUrl( String url ){
			this.url = url;
			return this;
		}
		
		public Resource setTitle( String title ){
			this.title = title;
			return this;
		}
		
		public Resource setBody( String body ){
			this.body = body;
			return this;
		}
	}
}
