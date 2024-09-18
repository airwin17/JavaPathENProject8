package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearAttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;
/**
 * Service that handles all the operations related to the users of the tour guide system.
 *
 * @author Aitor
 */
@Service
public class TourGuideService {

	/**
	 * Logger to log information about the service.
	 */
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);

	/**
	 * GPS utility to get the user location.
	 */
	private final GpsUtil gpsUtil;

	/**
	 * Service to calculate the rewards of the users.
	 */
	private final RewardsService rewardsService;

	/**
	 * Trip pricer to get the trip deals for the users.
	 */
	private final TripPricer tripPricer = new TripPricer();

	/**
	 * Tracker to track the user location.
	 */
	public final Tracker tracker;

	/**
	 * Flag to indicate if the test mode is enabled.
	 */
	boolean testMode = true;

	/**
	 * Constructor to initialize the service.
	 *
	 * @param gpsUtil
	 *            GPS utility to get the user location.
	 * @param rewardsService
	 *            Service to calculate the rewards of the users.
	 */
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		// Set the default locale to US.
		Locale.setDefault(Locale.US);

		if (testMode) {
			// Log information about the test mode.
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}

		// Initialize the tracker.
		tracker = new Tracker(this);

		// Add a shutdown hook to stop the tracker when the application is closed.
		addShutDownHook();
	}

	/**
	 * Get the rewards of the user.
	 *
	 * @param user
	 *            User to get the rewards.
	 * @return Map of rewards.
	 */
	public Map<String, UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * Get the location of the user.
	 *
	 * @param user
	 *            User to get the location.
	 * @return CompletableFuture with the location of the user.
	 * @throws InterruptedException
	 *             If the thread is interrupted.
	 * @throws ExecutionException
	 *             If an error occurs while executing the future.
	 */
	public CompletableFuture<VisitedLocation> getUserLocation(User user) throws InterruptedException, ExecutionException {
		if (user.getVisitedLocations().size() > 0) {
			return CompletableFuture.supplyAsync(() -> user.getLastVisitedLocation());
		} else {
			CompletableFuture<VisitedLocation> visitedLocation = trackUserLocation(user);
			
			return visitedLocation;
		}
	}

	/**
	 * Get the user by username.
	 *
	 * @param userName
	 *            Username of the user to get.
	 * @return User with the given username.
	 */
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	/**
	 * Get all the users.
	 *
	 * @return List of all users.
	 */
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().toList();
	}

	/**
	 * Add a user to the internal user map.
	 *
	 * @param user
	 *            User to add.
	 */
	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * Get the trip deals for the user.
	 *
	 * @param user
	 *            User to get the trip deals.
	 * @return List of trip deals.
	 */
	public List<Provider> getTripDeals(User user) {
		Map<String, UserReward> userRewards = user.getUserRewards();
		
		int cumulatativeRewardPoints = 0;
		for (UserReward userReward : userRewards.values()) {
			cumulatativeRewardPoints += cumulatativeRewardPoints + userReward.getRewardPoints();
		}
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Track the user location.
	 *
	 * @param user
	 *            User to track the location.
	 * @return CompletableFuture with the new location of the user.
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		
		return CompletableFuture.supplyAsync(() -> {
			VisitedLocation newLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(newLocation);
			rewardsService.calculateRewardsThread(user).start();
			return newLocation;
		});
	}

	/**
	 * Get the nearby attractions of the user.
	 *
	 * @param CompletableFuture with the visitedLocation
	 *            Visited location of the user.
	 * @return Array of nearby attractions.
	 */
	public CompletableFuture<NearAttractionDto[]> getNearByAttractions(CompletableFuture<VisitedLocation> visitedLocationf) {
		return visitedLocationf.thenApplyAsync(visitedLocation -> {
			List<Attraction> Attractions = gpsUtil.getAttractions();
		NearAttractionDto[] nearByAttractions = new NearAttractionDto[5];
		Attractions.sort((o1, o2) -> {
			double l1 = rewardsService.getDistance(o1, visitedLocation.location);
			double l2 = rewardsService.getDistance(o2, visitedLocation.location);
			return Double.compare(l1, l2);
		});
		
		for (int i = 0; i < nearByAttractions.length; i++) {
			NearAttractionDto nearAttractionDto = new NearAttractionDto();
			nearAttractionDto.setUserLocations(visitedLocation.location);
			nearAttractionDto.setDistance(rewardsService.getDistance(Attractions.get(i), visitedLocation.location));
		}
		return nearByAttractions;
		});
		
	}

	/**
	 * Add a shutdown hook to stop the tracker when the application is closed.
	 */
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**
	 * Methods below: For internal testing
	 */

	/**
	 * API key to get the trip deals.
	 */
	private static final String tripPricerApiKey = "test-server-api-key";

	/**
	 * Map to store the internal users.
	 */
	private final Map<String, User> internalUserMap = new HashMap<>();

	/**
	 * Initialize the internal users.
	 */
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	/**
	 * Generate a random user location history.
	 *
	 * @param user
	 *            User to generate the location history.
	 */
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	/**
	 * Generate a random latitude.
	 *
	 * @return Random latitude.
	 */
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	/**
	 * Generate a random longitude.
	 *
	 * @return Random longitude.
	 */
	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	/**
	 * Generate a random time.
	 *
	 * @return Random time.
	 */
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
}

