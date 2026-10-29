package dev.gunnerstv.launcher;

/** Provider-neutral choices based on published session metadata and capabilities. */
final class MediaSessionPolicy {
    enum Command { PLAY,PAUSE,TOGGLE,NONE }
    static String first(CharSequence... values){for(CharSequence v:values)if(v!=null&&!v.toString().trim().isEmpty())return v.toString().trim();return "";}
    static int rank(boolean playing,boolean buffering,boolean paused,boolean title,boolean music){
        if(!playing&&!buffering&&!paused&&!title)return -1;
        return (playing?300:buffering?200:paused?100:10)+(title?20:0)+(music?3:0);
    }
    static Command command(boolean playing,boolean play,boolean pause,boolean toggle){
        if(toggle)return Command.TOGGLE;
        if(playing&&pause)return Command.PAUSE;
        if(!playing&&play)return Command.PLAY;
        return Command.NONE;
    }
}
