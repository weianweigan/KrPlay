import com.krplay.protocol.HttpBody;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class HttpBodyTest {
    private static long read(String data,long limit)throws Exception {return HttpBody.read(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)),limit,n->{});}
    public static void main(String[] args)throws Exception {
        if(read("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello",100)!=5)throw new AssertionError();
        if(read("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3;ext=1\r\nabc\r\n2\r\nde\r\n0\r\n\r\n",100)!=5)throw new AssertionError();
        if(read("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\n1234567890",4)!=4)throw new AssertionError("download cap");
        for(String malformed:new String[]{"HTTP/1.1 503 Bad\r\n\r\n","HTTP/1.1 200 OK\r\nContent-Length: 9\r\n\r\nabc","HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\nx\r\n","HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\n\r\n"}){
            try{read(malformed,100);throw new AssertionError("malformed response accepted");}catch(IOException expected){}
        }
        System.out.println("HTTP download tests passed: lengths, chunking, caps, truncated/error/compressed responses");
    }
}
