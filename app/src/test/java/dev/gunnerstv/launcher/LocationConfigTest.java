package dev.gunnerstv.launcher;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocationConfigTest {
 @Test public void acceptsBoundaryCoordinates(){assertArrayEquals(new double[]{-90,180},LocationConfig.coordinates("-90","180"),0);}
 @Test public void rejectsNonFiniteValues(){assertNull(LocationConfig.coordinates("NaN","0"));assertNull(LocationConfig.coordinates("0","Infinity"));}
 @Test public void rejectsOutOfRange(){assertNull(LocationConfig.coordinates("90.01","0"));assertNull(LocationConfig.coordinates("0","-180.01"));}
 @Test public void emptyOrMalformedFallsBack(){assertNull(LocationConfig.coordinates("",""));assertNull(LocationConfig.coordinates(null,"0"));assertNull(LocationConfig.coordinates("north","west"));}
}
