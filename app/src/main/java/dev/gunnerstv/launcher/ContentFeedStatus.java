package dev.gunnerstv.launcher;

/** Safe diagnostics: counts and access states, never titles, links or account data. */
final class ContentFeedStatus {
    boolean listingsGranted;
    int previewRows,watchRows,readTables,deniedTables,failedTables,hiddenRows,unsupportedRows,invalidRows;
    boolean needsSetup(boolean sources,boolean items){return sources&&(!listingsGranted||deniedTables>0||failedTables>0||!items);}
    String message(){
        if(deniedTables>0)return listingsGranted?"The firmware denied one or more TV content tables despite listings access. Private Google TV/provider feeds cannot be read by this app.":"TV listings access is required to read content shared by other apps.";
        if(failedTables>0)return "A TV content table is unavailable. Refresh or reopen the provider apps.";
        if(!listingsGranted)return "TV listings access is not granted. Only public/own rows may be visible.";
        if(previewRows+watchRows==0)return "Access is granted, but the provider returned no readable programs. Apps must publish searchable shared content.";
        return "Shared TV tables are readable. Missing provider rows may be unpublished, hidden or lack playable links.";
    }
    String counts(){return "Readable tables: "+readTables+" · Preview records: "+previewRows+" · Watch Next records: "+watchRows+"\nHidden: "+hiddenRows+" · Other providers: "+unsupportedRows+" · Incomplete records: "+invalidRows;}
}
