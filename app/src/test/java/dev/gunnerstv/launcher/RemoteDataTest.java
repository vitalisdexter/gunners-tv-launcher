package dev.gunnerstv.launcher;
import java.io.*;
import java.net.URI;
import org.junit.Test;
import static org.junit.Assert.*;
public class RemoteDataTest {
 @Test public void exactLimitWorks()throws Exception{assertEquals(8,RemoteData.read(new ByteArrayInputStream(new byte[8]),8).length);}
 @Test public void oversizedResponseIsRejectedAndClosed(){final boolean[] closed={false};InputStream in=new ByteArrayInputStream(new byte[9]){public void close(){closed[0]=true;}};assertThrows(IOException.class,()->RemoteData.read(in,8));assertTrue(closed[0]);}
 @Test public void rejectsPrivateFilesCredentialsAndMalformedUris(){for(String s:new String[]{"file:///data/data/app/private","https://user:password@example.com/a","javascript:x","https://"})assertThrows(IOException.class,()->RemoteData.address(s,true));}
 @Test public void httpIsExplicitOnly()throws Exception{assertEquals("http",RemoteData.address("http://example.com/a",true).getScheme());assertThrows(IOException.class,()->RemoteData.address("http://example.com/a",false));}
 @Test public void downgradeAndUnsafeRedirectsAreRejected(){URI u=URI.create("https://example.com/a");for(String s:new String[]{"http://example.com/b","file:///tmp/x","https://u:p@example.com/a"})assertThrows(IOException.class,()->RemoteData.redirect(u,s,true));}
 @Test public void relativeRedirectWorks()throws Exception{assertEquals("https://example.com/b",RemoteData.redirect(URI.create("https://example.com/a"),"/b",false).toString());}
 @Test public void rejectsImageBombs(){assertThrows(IOException.class,()->RemoteData.imageSample(20000,20000,640));assertThrows(IOException.class,()->RemoteData.imageSample(-1,100,640));}
 @Test public void tallImagesAreSampled()throws Exception{int s=RemoteData.imageSample(64,8192,512);assertTrue(8192/s<=512);}
}
