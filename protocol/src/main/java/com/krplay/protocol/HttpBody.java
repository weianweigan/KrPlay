package com.krplay.protocol;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Bounded HTTP/1.1 response decoder used by the explicit tunnel download test. */
public final class HttpBody {
    private HttpBody() {}
    public static String line(InputStream input) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();int previous=-1;
        for(int i=0;i<8192;i++){int c=input.read();if(c<0)throw new EOFException();if(previous=='\r'&&c=='\n'){byte[] data=b.toByteArray();return new String(data,0,data.length-1,StandardCharsets.US_ASCII);}b.write(c);previous=c;}
        throw new IOException("HTTP header exceeds limit");
    }
    public interface Progress { void bytes(long count); }
    public static long read(InputStream input,long limit,Progress progress) throws IOException {
        String status=line(input);if(!status.matches("HTTP/1\\.[01] 2[0-9][0-9]( .*|$)"))throw new IOException("HTTP request failed: "+status);
        boolean chunked=false;long length=-1;
        for(int i=0;;i++){if(i>64)throw new IOException("Too many HTTP headers");String l=line(input);if(l.isEmpty())break;int p=l.indexOf(':');if(p<1)throw new IOException("Invalid HTTP header");String name=l.substring(0,p),value=l.substring(p+1).trim();
            if(name.equalsIgnoreCase("Transfer-Encoding"))chunked=value.equalsIgnoreCase("chunked");
            if(name.equalsIgnoreCase("Content-Length")){try{length=Long.parseLong(value);}catch(NumberFormatException e){throw new IOException(e);}if(length<0)throw new IOException("Invalid content length");}
            if(name.equalsIgnoreCase("Content-Encoding")&&!value.equalsIgnoreCase("identity"))throw new IOException("Compressed speed response");
        }
        byte[] buffer=new byte[16384];long total=0,remaining=chunked?0:length;
        while(total<limit){
            if(chunked&&remaining==0){String size=line(input).split(";",2)[0];try{remaining=Long.parseLong(size,16);}catch(NumberFormatException e){throw new IOException(e);}if(remaining<0)throw new IOException("Invalid chunk size");if(remaining==0)break;}
            if(!chunked&&remaining==0)break;
            int wanted=(int)Math.min(buffer.length,Math.min(limit-total,remaining<0?Long.MAX_VALUE:remaining));
            int n=input.read(buffer,0,wanted);if(n<0){if(remaining>0)throw new EOFException();break;}
            total+=n;if(remaining>0)remaining-=n;progress.bytes(total);
            if(chunked&&remaining==0&&!line(input).isEmpty())throw new IOException("Malformed chunk end");
        }
        return total;
    }
}
