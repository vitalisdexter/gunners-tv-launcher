package dev.gunnerstv.launcher;

/** Validates manual coordinates without Android, network requests or implicit geolocation. */
final class LocationConfig {
    static double[] coordinates(String latitude,String longitude){
        try{
            double lat=Double.parseDouble(latitude.trim()),lon=Double.parseDouble(longitude.trim());
            return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180?new double[]{lat,lon}:null;
        }catch(NullPointerException|NumberFormatException invalid){return null;}
    }
}
