package dev.gunnerstv.launcher;

import java.io.*;
import java.net.*;

/** Bounded network reads. Plain HTTP is retained only for app-published artwork. */
final class RemoteData {
    static byte[] read(InputStream in,int limit)throws IOException{
        if(in==null)throw new IOException("Missing stream");
        try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;
            while((n=stream.read(buffer))!=-1){if(n>limit-out.size())throw new IOException("Response exceeds limit");out.write(buffer,0,n);}
            return out.toByteArray();
        }
    }
    static URI address(String value,boolean allowHttp)throws IOException{
        try{
            URI uri=new URI(value);
            if(value.length()>8192||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null
                ||!("https".equalsIgnoreCase(uri.getScheme())||allowHttp&&"http".equalsIgnoreCase(uri.getScheme())))throw new IOException("Unsupported address");
            return uri;
        }catch(URISyntaxException|NullPointerException bad){throw new IOException("Malformed address");}
    }
    static URI redirect(URI previous,String location,boolean allowHttp)throws IOException{
        if(location==null)throw new IOException("Missing redirect");
        try{
            URI next=address(previous.resolve(location).toString(),allowHttp);
            if("https".equalsIgnoreCase(previous.getScheme())&&!"https".equalsIgnoreCase(next.getScheme()))throw new IOException("Insecure redirect");
            return next;
        }catch(IllegalArgumentException bad){throw new IOException("Malformed redirect");}
    }
    static byte[] get(String value,int limit,boolean allowHttp)throws IOException{
        URI uri=address(value,allowHttp);
        for(int hops=0;hops<=3;hops++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(7000);c.setRequestProperty("User-Agent","GunnersTV/1.0");
            try{
                int status=c.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){uri=redirect(uri,c.getHeaderField("Location"),allowHttp);continue;}
                if(status!=200)throw new IOException("HTTP "+status);
                if(c.getContentLengthLong()>limit)throw new IOException("Response exceeds limit");
                return read(c.getInputStream(),limit);
            }finally{c.disconnect();}
        }
        throw new IOException("Too many redirects");
    }
    static int imageSample(int width,int height,int target)throws IOException{
        if(width<=0||height<=0||width>16384||height>16384||(long)width*height>64_000_000L)throw new IOException("Invalid image dimensions");
        int sample=1;target=Math.max(1,Math.min(1024,target));
        while(Math.max(width,height)/sample>target)sample*=2;
        return sample;
    }
}
