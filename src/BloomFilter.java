//20140916
//PL
//SCUT Samsung Innovative Laboratory

//---URL排重模块---
//---不再从已爬取的URL下载HTML网页---
public class BloomFilter {
	//---Hash数组大小---
	public static final int SIZE = 24000000;
	
	//---模数，为SIZE的8倍---
	public static final int MAX = SIZE << 3;
	
	//---Hash数组---
	private char hash[] = new char[ SIZE ];
	
	//---获取第a位的值---
	public boolean getBit( char[] ch, long a ){
		return ( ch[ ( int )( a >> 3 ) ] & ( 1 << ( a & 7 ) ) ) > 0;
	}
	
	//---将第a位置为1---
	public void setBit( char[] ch, long a ){
		ch[ ( int )( a >> 3 ) ] |= 1 << ( a & 7 );
	}
	
	//---数组清零---
	public void clear(){
		for( int i = 0; i < SIZE; ++i ){
			hash[ i ] = '\0';
		}
	}
	
	//---检查URL是否已爬取过---
	//---若在6个哈希值表示位上均为1，则判别为已爬取---
	public boolean isUrlChecked( String url ){
		if( url == null || url.length() == 0 ) return true;
		
		char[] ch = url.toCharArray();
		int len = url.length();
		int flag = 0;
		long tmp;
		
		tmp = ( HFLPHash( ch, len ) % MAX + MAX ) % MAX;
		if( getBit( hash, tmp ) ) {      
            ++flag;  
        } else {  
        	setBit( hash, tmp );  
        }     
  
		tmp = ( StrHash( ch, len ) % MAX + MAX ) % MAX;
        if( getBit( hash, tmp ) ) {   
            ++flag;  
        } else {  
        	setBit( hash, tmp );  
        }  
         
        tmp = ( HFHash( ch, len ) % MAX + MAX ) % MAX;
        if( getBit( hash, tmp ) ) {  
            ++flag;  
        } else {  
        	setBit( hash, tmp );  
        }  
  
        tmp = ( JSHash( ch, len ) % MAX + MAX ) % MAX;
        if( getBit( hash, tmp ) ) {  
            ++flag;  
        } else {
        	setBit( hash, tmp );  
        }   
        
        tmp = ( ELFHash( ch, len ) % MAX + MAX ) % MAX;
        if( getBit( hash, tmp ) ) {  
            ++flag;  
        } else {  
        	setBit( hash, tmp );  
        }  
        
        tmp = ( SDBMHash( ch, len ) % MAX + MAX ) % MAX;
        if( getBit( hash, tmp ) ) {  
            ++flag;
        } else {  
            setBit( hash, tmp );
        } 
        
        return flag >= 6;
	}

	//---哈希函数1---
	public long JSHash( char[] str, int len ){
		long hash = 1315423911;
		for( int i = 0; i < len; ++i )
			hash ^= ( ( hash << 5 ) + str[ i ] + ( hash >> 2 ) );
		return ( hash & 0x7FFFFFFF );
	}

	//---哈希函数2---
	public long ELFHash( char[] str, int len ){
		long hash = 0;
		long x = 0;
		
		for( int i = 0; i < len; ++i ){
			hash = ( hash << 4 ) + str[ i ];
			if( ( x = hash & 0xF00000000L ) != 0 ) hash ^= ( x >> 24 );
			hash &= ~x;
		}
		return hash;
	}

	//---哈希函数3---
	public long SDBMHash( char[] str, int len ){
		long hash = 0;
		for( int i = 0; i < len; ++i ){
			hash = str[ i ] + ( hash << 6 ) + ( hash << 16 ) - hash;
		}
		return hash;
	}

	//---哈希函数4---
	public long HFLPHash( char[] str, int len ){
		char[] b = new char[ 4 ];
		long n = 0;
		for( int i = 0; i < len; ++i ) b[ i % 4 ] ^= str[ i ];
		n = ( ( long )b[0] << 24 ) | ( ( long )b[ 1 ] << 16 ) | ( ( long )b[ 2 ] << 8) | ( long )b[ 3 ];
		return n % len;
	}

	//---哈希函数5---
	public int HFHash( char[] str, int len ){
		int result = 0;
		for(int i = 1; i < len; ++i ) result = str[i]*3*i;
		if( result < 0 ) result = -result;
		return result % len;
	}

	//---哈希函数6---
	public long StrHash( char[] str, int len ){
		long h = 0;
		for( int i = 0; i < len; ++i )
			h = 31 * h + str[ i ];
		return h;
	}

}
