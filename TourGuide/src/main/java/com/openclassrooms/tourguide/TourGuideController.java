package com.openclassrooms.tourguide;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.dto.NearAttractionDto;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {
	private TourGuideService tourGuideService;
     public TourGuideController(TourGuideService tourGuideService) {
         this.tourGuideService = tourGuideService;
     }
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    @Async
    @RequestMapping("/getLocation") 
    public CompletableFuture<VisitedLocation> getLocation(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    @Async
    @RequestMapping("/getNearbyAttractions") 
    public CompletableFuture<NearAttractionDto[]> getNearbyAttractions(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	CompletableFuture<VisitedLocation> visitedLocation = tourGuideService.getUserLocation(getUser(userName));
    	return tourGuideService.getNearByAttractions(visitedLocation);
    }
    
    @RequestMapping("/getRewards") 
    public Map<String,UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}