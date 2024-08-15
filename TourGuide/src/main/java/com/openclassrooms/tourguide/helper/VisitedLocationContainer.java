package com.openclassrooms.tourguide.helper;

import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.user.User;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;

public class VisitedLocationContainer extends Thread implements ThreadContainer<VisitedLocation> {
private VisitedLocation value;
private GpsUtil gpsUtil;
private User user;
private RewardsService rewardsService;
public VisitedLocationContainer(GpsUtil gpsUtil,User user,RewardsService rewardsService) {
    this.gpsUtil = gpsUtil;
    this.user = user;
    this.rewardsService = rewardsService;
}
@Override
public void run() {
    VisitedLocation newLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(newLocation);
			rewardsService.calculateRewardsThread(user).run();
			value= newLocation;
}
@Override
public VisitedLocation getValue() {
    return value;
}
}
