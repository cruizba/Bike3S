package es.urjc.ia.bikesurbanfleets.users.types;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;
import es.urjc.ia.bikesurbanfleets.users.UserParameters;
import es.urjc.ia.bikesurbanfleets.users.UserType;
import es.urjc.ia.bikesurbanfleets.users.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents a user who doesn't know anything about the state of the system.
 * This user always chooses the closest destination station and the shortest route to reach it.
 * This user decides to leave the system randomly when a reservation fails if reservations are active
 *
 * @author IAgroup
 *
 */
@UserType("USER_UNINFORMED")
public class UserUninformed extends User {

    @UserParameters
    public class Parameters {

       //default constructor used if no parameters are specified
        private Parameters() {}
        
        /**
         * It is the number of times that the user will try to rent a bike (without a bike
         * reservation) before deciding to leave the system.
         */
         int minRentalAttempts = 3;

         int maxDistanceToRentBike = 600;

         int maxTimeOuts = 2;

         int maxFailedReservation = 2;

         GeoPoint intermediatePosition=null;

         boolean willReserve = false;
                
        @Override
        public String toString() {
            return "Parameters{" +
                    "minRentalAttempts=" + minRentalAttempts+
            '}';
        }

    }

    Parameters parameters;

    public UserUninformed(JsonObject userdef, SimulationServices services, long seed) throws Exception{
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
 
    //**********************************************
    //Decision related to reservations
    @Override
    public boolean decidesToLeaveSystemAfterTimeout() {
        return this.getMemory().getReservationTimeoutsCounter() >= parameters.maxTimeOuts;
    }
    @Override
    public boolean decidesToLeaveSystemAffterFailedReservation() {
        return this.getMemory().getReservationAttemptsCounter() >= parameters.maxFailedReservation;
    }
    @Override
    public boolean decidesToReserveBikeAtSameStationAfterTimeout() {
        return this.parameters.willReserve;
    }

    @Override
    public boolean decidesToReserveBikeAtNewDecidedStation() {
        return this.parameters.willReserve;
    }

    @Override
    public boolean decidesToReserveSlotAtSameStationAfterTimeout() {
        return this.parameters.willReserve;
    }

    @Override
    public boolean decidesToReserveSlotAtNewDecidedStation() {
        return this.parameters.willReserve;
    }

    @Override
    public boolean decidesToDetermineOtherStationAfterTimeout() {
        return true;
    }

    @Override
    public boolean decidesToDetermineOtherStationAfterFailedReservation() {
        return this.parameters.willReserve;
    }

    //**********************************************
    //decisions related to taking and leaving a bike
    @Override
    public boolean decidesToLeaveSystemWhenBikesUnavailable() {
        if (getMemory().getRentalAttemptsCounter() >= parameters.minRentalAttempts) 
            return true; 
        else return false;
     }

    @Override
    public Station determineStationToRentBike() {
        
        Station destination = null;
        List<Station> triedStations = getMemory().getStationsWithRentalFailedAttempts();
        List<Station> finalStations = informationSystem.getStationsBikeOrderedByDistanceNoFiltered(this.getPosition()).stream()
                .filter(station -> station.getPosition().distanceTo(this.getPosition()) <= parameters.maxDistanceToRentBike).collect(Collectors.toList());
        finalStations.removeAll(triedStations);

        if (!finalStations.isEmpty()) {
        	destination = finalStations.get(0);
        }
        return destination;
    }

    @Override
    public Station determineStationToReturnBike() {
        Station destination = null;
        List<Station> triedStations = getMemory().getStationsWithReturnFailedAttempts();
        List<Station> finalStations = informationSystem.getStationsBikeOrderedByDistanceNoFiltered(this.destinationPlace);
        finalStations.removeAll(triedStations);
        if (!finalStations.isEmpty()) {
        	destination = finalStations.get(0);
        } else {
            throw new RuntimeException("user cant return a bike, no slots");
        }
        return destination;
    }

    //**********************************************
    //decisions related to either go directly to the destination or going arround

    @Override
    public boolean decidesToGoToPointInCity() {
        if (parameters.intermediatePosition==null) return false;
        else return true;
    }

    @Override
    public GeoPoint getPointInCity() {
        return parameters.intermediatePosition;
    }

 
}

