package com.freightfox.tollplazafinder.util;

public class DistanceCalculator {
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public static double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    public static double[] findClosestPointOnSegment(
            double latA, double lonA,
            double latB, double lonB,
            double latP, double lonP) {

        double dLat = latB - latA;
        double dLon = lonB - lonA;

        if (dLat == 0.0 && dLon == 0.0) {
            return new double[]{latA, lonA, 0.0};
        }

        double meanLatRad = Math.toRadians((latA + latB) / 2.0);
        double cosMeanLat = Math.cos(meanLatRad);

        double vx = dLon * cosMeanLat;
        double vy = dLat;

        double ux = (lonP - lonA) * cosMeanLat;
        double uy = latP - latA;

        double lengthSquared = vx * vx + vy * vy;
        double t = (ux * vx + uy * vy) / lengthSquared;

        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }

        double closestLat = latA + t * dLat;
        double closestLon = lonA + t * dLon;

        return new double[]{closestLat, closestLon, t};
    }
}
