package dev.gunnerstv.launcher;

import org.json.*;
import java.util.*;

/** Private, versioned football preferences. Empty is an intentional supported selection. */
final class FootballSelection {
    static final int MAX_CLUBS=12,MAX_PLAYERS=8;
    static boolean id(String id){return id!=null&&id.matches("[1-9][0-9]{0,9}");}
    static String clean(String value){return value==null?"":value.replaceAll("[\\p{Cntrl}]","").trim();}
    static final class Team {
        final String id,name,detail;
        Team(String id,String name,String detail){this.id=id;this.name=clean(name);this.detail=clean(detail);if(!id(id)||this.name.isEmpty()||this.name.length()>120)throw new IllegalArgumentException("Invalid team");}
        JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("name",name).put("detail",detail);}
        static Team parse(JSONObject j){if(j==null)return null;try{return new Team(j.optString("id"),j.optString("name"),j.optString("detail"));}catch(IllegalArgumentException e){return null;}}
        String label(){return name+(detail.isEmpty()?"":" · "+detail);}
    }
    static final class Player {
        final String id,name;
        Team club,national;
        String scope="both";
        Player(String id,String name){this.id=id;this.name=clean(name);if(!id(id)||this.name.isEmpty()||this.name.length()>120)throw new IllegalArgumentException("Invalid player");}
        boolean club(){return !"international".equals(scope);}
        boolean international(){return !"club".equals(scope);}
        boolean valid(){return Arrays.asList("club","international","both").contains(scope)&&(!club()||club!=null)&&(!international()||national!=null);}
        String summary(){List<String> names=new ArrayList<>();if(club())names.add(club==null?"Club unavailable":club.name);if(international())names.add(national==null?"National side unavailable":national.name);return String.join(" + ",names);}
        Player copy(){try{return parse(json());}catch(JSONException e){throw new IllegalStateException(e);}}
        JSONObject json()throws JSONException{JSONObject j=new JSONObject().put("id",id).put("name",name).put("scope",scope);if(club!=null)j.put("club",club.json());if(national!=null)j.put("national",national.json());return j;}
        static Player parse(JSONObject j){if(j==null)return null;try{Player p=new Player(j.optString("id"),j.optString("name"));p.club=Team.parse(j.optJSONObject("club"));p.national=Team.parse(j.optJSONObject("national"));p.scope=j.optString("scope","both");return p.valid()?p:null;}catch(IllegalArgumentException e){return null;}}
    }
    final LinkedHashMap<String,Team> clubs=new LinkedHashMap<>();
    final LinkedHashMap<String,Player> players=new LinkedHashMap<>();
    String main="";
    boolean add(Team t){if(t==null||(!clubs.containsKey(t.id)&&clubs.size()>=MAX_CLUBS))return false;clubs.put(t.id,t);return true;}
    boolean add(Player p){if(p==null||!p.valid()||(!players.containsKey(p.id)&&players.size()>=MAX_PLAYERS))return false;players.put(p.id,p);return true;}
    boolean main(Team t){if(t==null){main="";return true;}if(!add(t))return false;main=t.id;return true;}
    void removeClub(String id){clubs.remove(id);if(main.equals(id))main="";}
    String mainName(){Team t=clubs.get(main);return t==null?"No main club":t.name;}
    boolean empty(){return clubs.isEmpty()&&players.isEmpty();}
    String summary(){return clubs.size()+" clubs · "+players.size()+" players";}
    String encode(){try{JSONArray c=new JSONArray(),p=new JSONArray();for(Team t:clubs.values())c.put(t.json());for(Player a:players.values())p.put(a.json());return new JSONObject().put("schema",1).put("main",clubs.containsKey(main)?main:"").put("clubs",c).put("players",p).toString();}catch(JSONException e){throw new IllegalStateException(e);}}
    static FootballSelection decode(String raw){
        FootballSelection out=new FootballSelection();try{JSONObject j=new JSONObject(raw);if(j.optInt("schema")!=1)return out;
            JSONArray c=j.optJSONArray("clubs"),p=j.optJSONArray("players");if(c!=null)for(int i=0;i<Math.min(c.length(),100);i++)out.add(Team.parse(c.optJSONObject(i)));
            if(p!=null)for(int i=0;i<Math.min(p.length(),100);i++)out.add(Player.parse(p.optJSONObject(i)));
            String main=j.optString("main");if(out.clubs.containsKey(main))out.main=main;
        }catch(JSONException|NullPointerException ignored){}return out;
    }
}
