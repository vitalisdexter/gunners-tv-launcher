package dev.gunnerstv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;

/** One discoverable access/status action even when every provider row is hidden. */
final class ContentSetupWidget extends Widget {
    final TextView detail;
    ContentSetupWidget(Context c,WidgetBoard.Host host){
        super(c,host,864);addView(text("Streaming content",19,ink(0xFF),"sans-serif-medium"),pos(800,27,24,22));
        detail=text("Checking shared content…",13,ink(0xB3),"sans-serif");detail.setSingleLine(false);detail.setMaxLines(3);addView(detail,pos(800,66,24,60));
        TextView hint=text("OK · Access, refresh and provider status",12,ink(0xD9),"sans-serif-medium");hint.setGravity(Gravity.CENTER);addView(hint,pos(800,25,24,142));
        control(this,0,1.012f,f->setBackground(round(f?(light()?0xFFE0E7F0:0xFF334657):restFill(),22)),()->new String[]{"content-status","Streaming content","Read app-shared recommendations and resume titles. Check access and provider availability."},()->((MainActivity)host.activity()).contentSources());
        refresh();
    }
    @Override void refresh(){ContentFeedReader.Result r=FeedCatalog.latest;detail.setText(r==null?"Check TV listings access and refresh your streaming apps' shared content.":r.status.message());}
}
