package es.urjc.ia.bikesurbanfleets.worldentities.users.types;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserType;
import es.urjc.ia.bikesurbanfleets.worldentities.users.User;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserDecision;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserDecisionGoToPointInCity;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserDecisionGoToStation;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserDecisionLeaveSystem;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserDecisionReserveBike;


import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a tourist, so this user, after renting a bike, cycles
 * to some place in the city in order to visit it. Then, this user never decides
 * to return directly the bike just after renting it. This type of user always
 * chooses the longest route when he has rented a bike.
 *
 * @author IAgroup
 *
 */
@UserType("USER_TOURIST")
public class UserTourist extends User {

    @UserParameters
    public class Parameters {

        /**
         * It indicates the size of the set of stations closest to the user
         * within which the destination will be chossen randomly.
         */
        private final int SELECTION_STATIONS_SET = 3;

        /**
         * It is the maximum time in seconds until which the user will decide to
         * continue walking or cycling towards the previously chosen station
         * witohout making a new reservation after a reservation timeout event
         * has happened.
         */
        private final double MIN_ARRIVALTIME_TO_RESERVE_AT_SAME_STATION = 180;

        /**
         * It is the place the tourist wants to visit after renting a b ike.
         */
        private GeoPoint touristDestination = null;

        /**
         * It is the number of times that a reservation timeout event musts
         * occurs before the user decides to leave the system.
         */
        private double minReservationTimeouts = rando.nextInt(1, 3);

        /**
         * It is the number of times that the user tries to rent a bike (without
         * a bike reservation) before deciding to leave the system.
         */
        private int minRentalAttempts = rando.nextInt(3, 6);

        /**
         * It determines the rate with which the user will reserve a bike.
         */
        private double bikeReservationPercentage = 50;

        /**
         * It determines the rate with which the user will reserve a slot.
         */
        private double slotReservationPercentage = 50;

        /**
         * It determines the rate with which the user will choose a new
         * destination station after a timeout event happens.
         */
        private double reservationTimeoutPercentage = 50;

        /**
         * It determines the rate with which the user will choose a new
         * destination station after he hasn't been able to make a reservation.
         */
        private double failedReservationPercentage = 50;

        //default constructor used if no parameters are specified
        private Parameters() {
        }

        @Override
        public String toString() {
            return "UserTouristParameters{"
                    + "SELECTION_STATIONS_SET=" + SELECTION_STATIONS_SET
                    + ", MIN_ARRIVALTIME_TO_RESERVE_AT_SAME_STATION=" + MIN_ARRIVALTIME_TO_RESERVE_AT_SAME_STATION
                    + ", touristDestination=" + touristDestination
                    + ", minReservationTimeouts=" + minReservationTimeouts
                    + ", minRentalAttempts=" + minRentalAttempts
                    + ", bikeReservationPercentage=" + bikeReservationPercentage
                    + ", slotReservationPercentage=" + slotReservationPercentage
                    + ", reservationTimeoutPercentage=" + reservationTimeoutPercentage
                    + ", failedReservationPercentage=" + failedReservationPercentage
                    + '}';
        }
    }

    private Parameters parameters;

    public UserTourist(JsonObject userdef, SimulationServices services, long seed) throws Exception {
        super(services, userdef, seed);
        //***********Parameter treatment*****************************
        //if this user has parameters this is the right declaration
        //if no parameters are used this code just has to be commented
        //"getparameters" is defined in USER such that a value of Parameters 
        // is overwritten if there is a values specified in the jason description of the user
        // if no value is specified in jason, then the orriginal value of that field is mantained
        // that means that teh paramerts are all optional
        // if you want another behaviour, then you should overwrite getParameters in this calss
        this.parameters = new Parameters();
        getParameters(userdef.getAsJsonObject("userType"), this.parameters);
    }

    /**
     * It randomly chooses a station among the pre-established number of nearest
     * stations.
     * @return 
     */
    @Override
    protected Station determineStationToRentBike() {
        List<Station> recommendedStations = informationSystem.getStationsWithAvailableBikesOrderedByWalkDistance(this.getPosition());
        Station destination = null;

        if (!recommendedStations.isEmpty()) {
            List<Station> nearestStations = new ArrayList<>();

            int end = parameters.SELECTION_STATIONS_SET < recommendedStations.size()
                    ? parameters.SELECTION_STATIONS_SET : recommendedStations.size();

            for (int i = 0; i < end; i++) {
                nearestStations.add(recommendedStations.get(i));
            }

            int index = rando.nextInt(0, end - 1);
            destination = nearestStations.get(index);
        }
        return destination;
    }

    /**
     * It randomly chooses a station among the pre-established number of nearest
     * stations.
     * @return 
     */
    @Override
    protected Station determineStationToReturnBike() {
        List<Station> recommendedStations = informationSystem.getStationsWithAvailableSlotsOrderedByDistance(this.getPosition(),"bike");

        int end = parameters.SELECTION_STATIONS_SET < recommendedStations.size()
                ? parameters.SELECTION_STATIONS_SET : recommendedStations.size();
        List<Station> nearestStations = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            nearestStations.add(recommendedStations.get(i));
        }
        int index = rando.nextInt(0, end - 1);
        return nearestStations.get(index);
    }
    
    @Override
    public String toString() {
        return super.toString() + "UserDistanceRestriction{"
                + "parameters=" + parameters
                + '}';
    }

    @Override
    public UserDecision decideAfterAppearning() {
        Station s = determineStationToRentBike();
        int percentage = rando.nextInt(0, 100);
        boolean reserve = percentage < parameters.bikeReservationPercentage;
            if (reserve) return new UserDecisionReserveBike(s);
            else return new UserDecisionGoToStation(s);
    }

    @Override
    public UserDecision decideAfterFailedRental() {
        if (getMemory().getRentalAttemptsCounter() >= parameters.minRentalAttempts) {
            return new UserDecisionLeaveSystem();
        } else {
            Station s = determineStationToRentBike();
            int percentage = rando.nextInt(0, 100);
            boolean reserve = percentage < parameters.bikeReservationPercentage;
            if (reserve) return new UserDecisionReserveBike(s);
            else return new UserDecisionGoToStation(s);
        }
    }

    @Override
    public UserDecision decideAfterFailedBikeReservation() {
        int percentage = rando.nextInt(0, 100);
        boolean gotooldstation = percentage < parameters.failedReservationPercentage;
        Station s;
        if (gotooldstation) {
            s = this.getDestinationStation();
            return new UserDecisionGoToStation(s);
        } else {
            s = determineStationToRentBike();
            percentage = rando.nextInt(0, 100);
            boolean reserve = percentage < parameters.bikeReservationPercentage;
            if (reserve) return new UserDecisionReserveBike(s);
            else return new UserDecisionGoToStation(s);
        }
    }

    @Override
    public UserDecision decideAfterBikeReservationTimeout() {
        int arrivalTime = timeToReach();
        boolean reserveAtSame = arrivalTime < parameters.MIN_ARRIVALTIME_TO_RESERVE_AT_SAME_STATION ? false : rando.nextBoolean();

        if (reserveAtSame) {
            return new UserDecisionReserveBike(this.getDestinationStation());
        } else {
            Station s = determineStationToRentBike();
            int percentage = rando.nextInt(0, 100);
            boolean reserve = percentage < parameters.bikeReservationPercentage;
            if (reserve) return new UserDecisionReserveBike(s);
            else return new UserDecisionGoToStation(s);
        }
     }

    @Override
    public UserDecision decideAfterGettingBike() {
        if (parameters.touristDestination != null) {
            return new UserDecisionGoToPointInCity(parameters.touristDestination);
        } else {
            Station s = determineStationToReturnBike();
            return new UserDecisionGoToStation(s);
        }
    }

    @Override
    public UserDecision decideAfterFailedReturn() {
        Station s = determineStationToReturnBike();
        return new UserDecisionGoToStation(s);
    }

    @Override
    public UserDecision decideAfterFinishingRide() {
        Station s = determineStationToReturnBike();
        return new UserDecisionGoToStation(s);
    }

    @Override
    public UserDecision decideAfterFailedSlotReservation() {
        return null;
    }

    @Override
    public UserDecision decideAfterSlotReservationTimeout() {
        return null;
    }

}
