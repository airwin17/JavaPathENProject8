package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.helper.VisitedLocationContainer;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

public class TestPerformance {

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remains consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100,000 users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	 @Test
	 @Disabled
	 public void highVolumeTrackLocation() throws InterruptedException {
		 GpsUtil gpsUtil = new GpsUtil();
		 RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		 // Users should be incremented up to 100,000, and test finishes within 15
		 // minutes
		 InternalTestHelper.setInternalUserNumber(100000);
		 TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
 
		 List<User> allUsers = new ArrayList<>();
		 allUsers = tourGuideService.getAllUsers();
 
		 StopWatch stopWatch = new StopWatch();
		 stopWatch.start();
		List<VisitedLocationContainer> future = new LinkedList<>();
		 for (User user : allUsers) {
			VisitedLocationContainer visitedLocation = tourGuideService.trackUserLocation(user);
			visitedLocation.start();
			future.add(visitedLocation);
		 }
		 for(int i = 0; i < future.size(); i++) {
			VisitedLocationContainer visitedLocation = future.get(i);
			visitedLocation.join();
		 }
		 stopWatch.stop();
		 tourGuideService.tracker.stopTracking();
 
		 System.out.println("highVolumeTrackLocation: Time Elapsed: "
				 + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		 assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 }

	@Test
	@Disabled
	public void highVolumeGetRewards() throws InterruptedException {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Users should be incremented up to 100,000, and test finishes within 20
		// minutes
		InternalTestHelper.setInternalUserNumber(100000);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Attraction attraction = gpsUtil.getAttractions().get(0);
		List<User> allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
		
		Thread[] futures = new Thread[allUsers.size()];
		
		for(int i = 0; i < futures.length; i++) {
			futures[i] = rewardsService.calculateRewardsThread(allUsers.get(i));
			futures[i].start();
		}
		for(int i = 0; i < futures.length; i++) {
			futures[i].join();
		}
		for(int i = 0; i < allUsers.size(); i++) {
			assertTrue(allUsers.get(i).getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
				+ " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

}
