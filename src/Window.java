//20140921
//PL
//SCUT Samsung Innovative Laboratory

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;


public class Window extends JFrame {

	private static final long serialVersionUID = 1L;
	private Window thisWindow;
	private JPanel contentPane;
	private JTextField textField1;
	private JTextField textField2;
	private JTable table1;
	private JTable table2;
	private JLabel size1;
	private JLabel size2;
	private JLabel size3;
	private JTextPane seedUrls;

	//---爬虫模块---
	private Crawler crawler;
	
	//---运行状态标记---
	private static final int RUNNING = 0;
	private static final int PAUSING = 1;
	private static final int STOPPED = 2;
	private int flag = STOPPED;

	//---设置URL queue size---
	public void setUrlQueueSize( int a ){
		size1.setText( a + " URL(s)" );
	}

	//---设置HTML queue size---
	public void setHtmlQueueSize( int a ){
		size2.setText( a + " HTML(s)" );
	}
	
	//---设置已缓存页面数---
	public void setCachedPage( int a ){
		size3.setText( "cached page: " + a + " page(s)" );
	}
	
	//---返回seed URL---
	public String[] getSeedUrls(){
		LinkedList< String > list = new LinkedList< String >();
		String tmp;
		String[] res = null;
		File file = new File( "seedURL.txt" );
		PrintStream ps = null;
		BufferedReader br = null;
		
		if( !file.exists() ){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			ps = new PrintStream( new FileOutputStream( file ) );
			ps.print( seedUrls.getText() );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally{
			if( ps != null ){
				ps.close();
				ps = null;
			}
		}
		
		try {
			br = new BufferedReader( new FileReader( file ) );
			while( ( tmp = br.readLine() ) != null ){
				list.add( tmp );
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if( br != null ){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				br = null;
			}
		}
		
		if( list.size() > 0 ){
			res = new String[ list.size() ];
			int i = 0;
			for( String element : list ){
				res[ i++ ] = element;
			}
		}
		
		return res;
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Window frame = new Window();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Window() {
		thisWindow = this;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 722, 478);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblHttpclient = new JLabel("client thread\uFF1A");
		lblHttpclient.setBounds(10, 10, 93, 15);
		contentPane.add(lblHttpclient);
		
		textField1 = new JTextField();
		textField1.setBounds(100, 7, 66, 21);
		textField1.setText( "1" );
		contentPane.add(textField1);
		textField1.setColumns(10);
		
		JLabel lblHtmlparser = new JLabel("parser thread:");
		lblHtmlparser.setBounds(183, 10, 93, 15);
		contentPane.add(lblHtmlparser);
		
		textField2 = new JTextField();
		textField2.setBounds(275, 7, 66, 21);
		textField2.setText( "1" );
		contentPane.add(textField2);
		textField2.setColumns(10);
		
		//---start button---
		JButton btnStart = new JButton("start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if( flag == STOPPED ){
					crawler = new Crawler( Integer.parseInt( textField1.getText() ), Integer.parseInt( textField2.getText() ) );
					crawler.work();
				}
				else if( flag == PAUSING ){
					crawler.start();
				}
			}
		});
		btnStart.setBounds(394, 6, 93, 23);
		contentPane.add(btnStart);
		
		//---pause button---
		JButton btnPause = new JButton("pause");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( crawler != null ) crawler.pause();
			}
		});
		btnPause.setBounds(497, 6, 93, 23);
		contentPane.add(btnPause);
		
		//---stop button---
		JButton btnStop = new JButton("stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( crawler != null ) crawler.stop();
				System.exit( 0 );
			}
		});
		btnStop.setBounds(600, 6, 93, 23);
		contentPane.add(btnStop);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 213, 333, 217);
		contentPane.add(scrollPane);
		
		table1 = new JTable();
		table1.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"client ID", "status"
			}
		));
		table1.getColumnModel().getColumn(0).setPreferredWidth(15);
		table1.getColumnModel().getColumn(1).setPreferredWidth(200);
		scrollPane.setViewportView(table1);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(353, 213, 340, 217);
		contentPane.add(scrollPane_1);
		
		table2 = new JTable();
		table2.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"parser ID", "status"
			}
		));
		table2.getColumnModel().getColumn(0).setPreferredWidth(15);
		table2.getColumnModel().getColumn(1).setPreferredWidth(200);
		scrollPane_1.setViewportView(table2);
		
		JLabel lblUrlQueueSize = new JLabel("URL queue size:");
		lblUrlQueueSize.setBounds(10, 188, 107, 15);
		contentPane.add(lblUrlQueueSize);
		
		size1 = new JLabel("0");
		size1.setBounds(119, 188, 222, 15);
		contentPane.add(size1);
		
		JLabel lblHtmlQueueSize = new JLabel("HTML queue size:");
		lblHtmlQueueSize.setBounds(353, 188, 107, 15);
		contentPane.add(lblHtmlQueueSize);
		
		size2 = new JLabel("0");
		size2.setBounds(472, 188, 221, 15);
		contentPane.add(size2);
		
		JLabel lblSeedUrls = new JLabel("seed URLs");
		lblSeedUrls.setBounds(10, 35, 331, 15);
		contentPane.add(lblSeedUrls);
		
		seedUrls = new JTextPane();
		seedUrls.setText("http://english.peopledaily.com.cn/");
		seedUrls.setBounds(10, 60, 331, 118);
		contentPane.add(seedUrls);
		
		size3 = new JLabel("cached page: 0");
		size3.setFont(new Font("SimSun", Font.BOLD, 15));
		size3.setBounds(394, 100, 299, 15);
		contentPane.add(size3);
	}
}
