package es.urjc.ia.bikesurbanfleets.worldentities.users;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoRoute;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Reservation.ReservationState;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Reservation.ReservationType;

/**
 * This class keeps track of the number of times that a same event has happend.
 * It saves information about negative facts, i. e., events which has not finally happened (failed reservations/rentals/rturns attempts).
 * It provides the corresponding method to update its counters.  
 * @author IAgroup
 *
 */
public class UserMemory {
    
    public static enum FactType {
        BIKE_RESERVATION_TIMEOUT, BIKE_FAILED_RESERVATION, BIKES_UNAVAILABLE, SLOTS_UNAVAILABLE,
        SLOT_RESERVATION_TIMEOUT, SLOT_FAILED_RESERVATION
    }
    
    /**
     * Times that a user has tried to reserve a bike and has not been able to.
     */
    private int bikeReservationAttemptsCounter;
    
    /**
     * Times that a user's bike reservation has expired (before renting the bike).  
     */
    private int bikeReservationTimeoutsCounter;

    /**
     * Times that a user has tried to reserve a slot and has not been able to.
     */
    private int slotReservationAttemptsCounter;
    
    /**
     * Times that a user's slot reservation has expired (before renting the bike).  
     */
    private int slotReservationTimeoutsCounter;

    /**
     * Times that a user has tried to rent a bike and has not been able to.
     */
    private int rentalAttemptsCounter;
    
    /**
     * Times that a user has tried to return the bike and has not been able to.
     */
    private int returnAttemptsCounter;

    private User user;

    private List<Station> stationsWithRentalFailedAttempts;
    private List<Station> stationsWithReturnFailedAttempts;
    private List<Station> stationsWithReservationRentalFailedAttempts;
    private List<Station> stationsWithReservationReturnFailedAttempts;
    private List<Reservation> reservations;
    private double distanceTraveledByBike;
    private double walkedtoTakeDistance;
  
    public UserMemory(User user) {
        this.bikeReservationAttemptsCounter = 0; 
        this.bikeReservationTimeoutsCounter = 0;
        this.rentalAttemptsCounter = 0;
        this.returnAttemptsCounter = 0;
        this.user = user;
        this.stationsWithRentalFailedAttempts = new ArrayList<>();
        this.stationsWithReturnFailedAttempts = new ArrayList<>();
        this.stationsWithReservationRentalFailedAttempts = new ArrayList<>();
        this.stationsWithReservationReturnFailedAttempts = new ArrayList<>();
        this.reservations = new ArrayList<>();
        this.distanceTraveledByBike = 0;
        this.walkedtoTakeDistance = 0;
    }
    
    public List<Reservation> getReservations() {
    	return reservations;
    }

    public int getReservationAttemptsCounter() {
        return bikeReservationAttemptsCounter;
    }

    public int getReservationTimeoutsCounter() {
        return bikeReservationTimeoutsCounter;
    }

    public int getRentalAttemptsCounter() {
        return rentalAttemptsCounter;
        
    }

    public int getReturnAttemptsCounter() {
        return returnAttemptsCounter;
    }

    public List<Station> getStationsWithRentalFailedAttempts() {
        return this.stationsWithRentalFailedAttempts;
    }

    public List<Station> getStationsWithReturnFailedAttempts() {
        return this.stationsWithReturnFailedAttempts;
    }
    
    public double getDistanceTraveledByBike() {
    	return distanceTraveledByBike;
    }
    
    public double getWalkedToTakeBikeDistance() {
    	return walkedtoTakeDistance;
    }
    
    public void setDistanceTraveledByBike(double distance) {
    	distanceTraveledByBike = distance;
    }
    
    public void setWalkedToTakeBikeDistance(double distance) {
    	walkedtoTakeDistance = distance;
    }
    public void addWalkedToTakeBikeDistance(double distance) {
    	walkedtoTakeDistance += distance;
    }
    
    public void update(FactType fact, Station s) throws IllegalArgumentException {
        switch(fact) {
            case BIKE_RESERVATION_TIMEOUT: bikeReservationTimeoutsCounter++;
            break;
            case BIKE_FAILED_RESERVATION: 
            	bikeReservationAttemptsCounter++;
            	stationsWithReservationRentalFailedAttempts.add(s);
            break;
            case SLOT_RESERVATION_TIMEOUT: slotReservationTimeoutsCounter++;
            break;
            case SLOT_FAILED_RESERVATION: 
            	slotReservationAttemptsCounter++;
            	stationsWithReservationReturnFailedAttempts.add(s);
            break;
            case BIKES_UNAVAILABLE:
                rentalAttemptsCounter++;
                stationsWithRentalFailedAttempts.add(s);
            break;
            case SLOTS_UNAVAILABLE:
                returnAttemptsCounter++;
                stationsWithReturnFailedAttempts.add(s);
            break;
            default: throw new IllegalArgumentException(fact.toString() + "is not defined in update method");
        }
    }
    
    /**
    * It obtains the stations for which a user has tried to make a bike reservation in an specific moment.
    * @param timeInstant it is the moment at which he has decided he wants to reserve a bike
    * and he has been trying it.
    * @return a list of stations for which the bike reservation has failed because of unavailable bikes.
    */    
    public List<Station> getStationsWithBikeReservationAttempts(int timeInstant) {
        return reservations.stream()
                .filter(reservation -> reservation.getType() == ReservationType.BIKE)
                .filter(reservation -> reservation.getState() == ReservationState.FAILED)
                .filter(reservation -> reservation.getStartInstant() == timeInstant)
                .map(Reservation::getStation)
                .collect(Collectors.toList());
    }
    
    public List<Station> getStationsWithSlotReservationAttempts(int timeInstant) {
        return reservations.stream()
                .filter(reservation -> reservation.getType() == ReservationType.SLOT)
                .filter(reservation -> reservation.getState() == ReservationState.FAILED)
                .filter(reservation -> reservation.getStartInstant() == timeInstant)
                .map(Reservation::getStation)
                .collect(Collectors.toList());
    }
    
    public double getTimeRidingABike() {
    	return distanceTraveledByBike / user.getCyclingVelocity();
    }
    
    public double getTimeWalking() {
    	return walkedtoTakeDistance / user.getWalkingVelocity();
    }

	public List<Station> getStationsWithReservationRentalFailedAttempts() {
		return stationsWithReservationRentalFailedAttempts;
	}


	public List<Station> getStationsWithReservationReturnFailedAttempts() {
		return stationsWithReservationReturnFailedAttempts;
	}


}
