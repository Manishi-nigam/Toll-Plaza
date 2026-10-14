package com.freightfox.tollplazafinder.util;

import java.util.ArrayList;
import java.util.List;

public class PolyLineDecoder {
    public static List<double[]> decode(String encodedPolyline){

        List<double[]> coordinates= new ArrayList<>();

        int index= 0;
        int latitude=0;
        int longitude=0;

        while (index < encodedPolyline.length()) {

            int result = 0;
            int shift = 0;
            int value;

            do {
                value = encodedPolyline.charAt(index++) - 63;
                result |= (value & 0x1f) << shift;
                shift += 5;
            } while (value >= 0x20);

            int deltaLatitude =
                    (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            latitude += deltaLatitude;

            result = 0;
            shift = 0;

            do {
                value = encodedPolyline.charAt(index++) - 63;
                result |= (value & 0x1f) << shift;
                shift += 5;
            } while (value >= 0x20);

            int deltaLongitude =
                    (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            longitude += deltaLongitude;

            double decodedLatitude = latitude / 1E5;
            double decodedLongitude = longitude / 1E5;

            coordinates.add(
                    new double[]{decodedLatitude, decodedLongitude}
            );
        }

        return coordinates;
    }
}
