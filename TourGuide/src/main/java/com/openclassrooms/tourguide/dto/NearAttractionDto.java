package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Location;

public class NearAttractionDto {

    private String TouristAttractionName;
    private String TouristAttractionLocation;
    private Location userLocations;
    private double distance;
    private int rewardPoints;
    
    public String getTouristAttractionName() {
        return TouristAttractionName;
    }
    public void setTouristAttractionName(String touristAttractionName) {
        TouristAttractionName = touristAttractionName;
    }
    public String getTouristAttractionLocation() {
        return TouristAttractionLocation;
    }
    public void setTouristAttractionLocation(String touristAttractionLocation) {
        TouristAttractionLocation = touristAttractionLocation;
    }
    public Location getUserLocations() {
        return userLocations;
    }
    public void setUserLocations(Location userLocations) {
        this.userLocations = userLocations;
    }
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public int getRewardPoints() {
        return rewardPoints;
    }
    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
