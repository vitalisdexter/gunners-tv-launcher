package dev.gunnerstv.launcher;

import org.json.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.*;

/** Public YouTube Atom feeds and standard RSS discovery metadata; no account/cookies/API key. */
final class YouTubeFeeds {
    static final long WEEK=7*86400000L;
    static final String ATOM="http://www.w3.org/2005/Atom",YT="http://www.youtube.com/xml/schemas/2015";
    static boolean channel(String id){return id!=null&&id.matches("UC[A-Za-z0-9_-]{22}");}
    static boolean video(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{11}");}
    static boolean host(String h){return "youtube.com".equals(h)||"www.youtube.com".equals(h)||"m.youtube.com".equals(h);}
    static String clean(String s,int max){s=s==null?"":s.replaceAll("[\\p{Cntrl}]","").trim();return s.length()>max?s.substring(0,max):s;}
    static String normalizeFeedId(String id){id=id==null?"":id.trim();return channel(id)?id:id.matches("[A-Za-z0-9_-]{22}")?"UC"+id:"";}
    static String directId(String input){
        input=input.trim();if(channel(input))return input;
        try{URI u=new URI(input);if(!"https".equals(u.getScheme())||!host(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1||u.getFragment()!=null)throw new IllegalArgumentException();
            String path=u.getPath();if(path.matches("/channel/UC[A-Za-z0-9_-]{22}/?"))return path.split("/")[2];
            if("/feeds/videos.xml".equals(path)&&u.getRawQuery()!=null)for(String pair:u.getRawQuery().split("&"))if(pair.startsWith("channel_id=")){String id=pair.substring(11);if(channel(id))return id;}
            if(path.matches("/@[\\p{L}\\p{N}._-]{3,100}(/(videos|streams|about))?/?")&&u.getRawQuery()==null)return "";
        }catch(URISyntaxException ignored){}throw new IllegalArgumentException("Use a public YouTube channel URL, @handle URL or UC channel ID");
    }
    static String discoveryId(String html){
        if(html.length()>3_000_000)throw new IllegalArgumentException("Channel page too large");
        Set<String> rss=new LinkedHashSet<>(),canonical=new LinkedHashSet<>();Matcher tags=Pattern.compile("<link\\b[^>]*>",Pattern.CASE_INSENSITIVE).matcher(html);
        Pattern attribute=Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:]*)\\s*=\\s*([\"'])(.*?)\\2",Pattern.DOTALL);
        while(tags.find()){Map<String,String> attrs=new HashMap<>();Matcher a=attribute.matcher(tags.group());while(a.find())attrs.put(a.group(1).toLowerCase(Locale.ROOT),a.group(3).replace("&amp;","&"));
            String rel=attrs.getOrDefault("rel","").toLowerCase(Locale.ROOT),href=attrs.getOrDefault("href","");
            try{if(rel.equals("canonical")){String id=directId(href);if(channel(id))canonical.add(id);}else if(rel.equals("alternate")&&"application/rss+xml".equals(attrs.get("type"))){String id=directId(href);if(channel(id))rss.add(id);}}catch(IllegalArgumentException ignored){}
        }
        if(canonical.size()!=1||rss.size()!=1||!rss.equals(canonical))throw new IllegalArgumentException("No unambiguous public RSS link; use the /channel/UC URL instead");return rss.iterator().next();
    }
    static byte[] get(String url,int limit)throws IOException{
        URI uri=RemoteData.address(url,false);
        for(int hop=0;hop<=3;hop++){
            if(!host(uri.getHost())||uri.getPort()!=-1)throw new IOException("YouTube host required");if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(7000);c.setRequestProperty("User-Agent","Mozilla/5.0 (compatible; GunnersTV/1.2)");
            try{int code=c.getResponseCode();if(code==301||code==302||code==303||code==307||code==308){uri=RemoteData.redirect(uri,c.getHeaderField("Location"),false);continue;}if(code!=200)throw new IOException("YouTube feed unavailable");if(c.getContentLengthLong()>limit)throw new IOException("Response too large");return RemoteData.read(c.getInputStream(),limit);}finally{c.disconnect();}
        }throw new IOException("Too many redirects");
    }
    static final class Video {
        final String id,title,author,channel;
        final long published;
        final Set<String> clubs=new LinkedHashSet<>();
        Video(String id,String title,String author,String channel,long published){this.id=id;this.title=clean(title,300);this.author=clean(author,120);this.channel=channel;this.published=published;}
        JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("title",title).put("author",author).put("channel",channel).put("published",published);}
        static Video json(JSONObject j){if(j==null||!video(j.optString("id"))||!channel(j.optString("channel")))return null;return new Video(j.optString("id"),j.optString("title"),j.optString("author"),j.optString("channel"),j.optLong("published"));}
    }
    static final class Feed {
        final String name;final List<Video> videos;
        Feed(String name,List<Video> videos){this.name=name;this.videos=videos;}
    }
    static String text(Element e,String ns,String tag){NodeList list=e.getElementsByTagNameNS(ns,tag);return list.getLength()==0?"":list.item(0).getTextContent();}
    static Feed parse(String xml,String expected,long now)throws Exception{
        if(!channel(expected)||xml.length()>1_000_000||Pattern.compile("<!\\s*(DOCTYPE|ENTITY)\\b",Pattern.CASE_INSENSITIVE).matcher(xml).find())throw new IOException("Unsafe feed");
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);DocumentBuilder b=f.newDocumentBuilder();b.setEntityResolver((pub,sys)->{throw new SAXException("External entities disabled");});
        Element root=b.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        if(!ATOM.equals(root.getNamespaceURI())||!"feed".equals(root.getLocalName())||!expected.equals(normalizeFeedId(text(root,YT,"channelId"))))throw new IOException("Wrong channel feed");
        String name=clean(text(root,ATOM,"title"),120);if(name.isEmpty())throw new IOException("Missing creator name");
        List<Video> out=new ArrayList<>();Set<String> ids=new HashSet<>();NodeList entries=root.getElementsByTagNameNS(ATOM,"entry");
        for(int i=0;i<Math.min(entries.getLength(),30);i++)try{Element e=(Element)entries.item(i);String id=text(e,YT,"videoId"),title=clean(text(e,ATOM,"title"),300);long at=OffsetDateTime.parse(text(e,ATOM,"published")).toInstant().toEpochMilli();
            if(!video(id)||title.isEmpty()||!expected.equals(normalizeFeedId(text(e,YT,"channelId")))||at<now-WEEK||at>now+5*60000L||!ids.add(id))continue;out.add(new Video(id,title,name,expected,at));
        }catch(RuntimeException malformed){}
        out.sort(Comparator.comparingLong((Video v)->v.published).reversed().thenComparing(v->v.id));return new Feed(name,out);
    }
    static Feed fetch(String id,long now)throws Exception{return parse(new String(get("https://www.youtube.com/feeds/videos.xml?channel_id="+id,1_000_000),StandardCharsets.UTF_8),id,now);}
    static final class Channel {final String id,name;Channel(String id,String name){this.id=id;this.name=name;}}
    static Channel resolve(String input)throws Exception{String id=directId(input);if(id.isEmpty()){URI u=new URI(input.trim());String page="https://www.youtube.com"+u.getRawPath();id=discoveryId(new String(get(page,3_000_000),StandardCharsets.UTF_8));}Feed feed=fetch(id,System.currentTimeMillis());return new Channel(id,feed.name);}
    static String watch(String id){if(!video(id))throw new IllegalArgumentException("Invalid video");return "https://www.youtube.com/watch?v="+id;}
}
