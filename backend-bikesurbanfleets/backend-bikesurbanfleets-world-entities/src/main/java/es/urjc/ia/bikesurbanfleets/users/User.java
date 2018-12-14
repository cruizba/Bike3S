package es.urjc.ia.bikesurbanfleets.users;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoRoute;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphManager;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GeoRouteCreationException;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GeoRouteException;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GraphHopperIntegrationException;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Entity;
import es.urjc.ia.bikesurbanfleets.common.util.IdGenerator;
import es.urjc.ia.bikesurbanfleets.common.util.SimpleRandom;
import es.urjc.ia.bikesurbanfleets.consultSystems.InformationSystem;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Bike;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is the main entity of the the system. It represents the basic behaviour
 * of all users type that can appear at the system. It provides an
 * implementation for basic methods which manage common information for all kind
 * of users. It provides a behaviour pattern (make decissions) which depends on
 * specific user type properties.
 *
 * @author IAgroup
 */
public abstract class User implements Entity {

    public static IdGenerator idgenerator;
    
    public static void resetIdGenerator(){
        idgenerator=new IdGenerator();
    }

    public enum STATE {
        APPEARED, TRY_BIKE_RESERVATION, WALK_TO_STATION, WITH_BIKE_TO_STATION, WITH_BIKE_ON_RIDE, TRY_SLOT_RESERVATION, WALK_TO_DESTINATION, EXIT_AFTER_TIMEOUT,
        LEAVING, LEFT_SYSTEM 
    }
    
    private int id;
    private STATE state;

    /**
     * Current user position.
     */
    private GeoPoint position;

    /**
     * Before user removes a bike or after returns it, this attribute is null.
     * While user is cycling, this attribute contains the bike the user has
     * rented.
     */
    private Bike bike;

    /**
     * It is the station to which user has decided to go at this moment.
     */
    private Station destinationStation;

    /**
     * It is the user destination in the city.
     */
    protected GeoPoint destinationPlace;

    /**
     * Speed in meters per second at which user walks.
     */
    protected double walkingVelocity;

    /**
     * Speed in meters per second at which user cycles.
     */
    protected double cyclingVelocity;

    /**
     * It is the user current (bike or slot) reservation, i. e., the last
     * reservation user has made. If user hasn't made a reservation, this
     * attribute is null.
     */
    private Reservation reservation;

    /**
     * It is the route that the user is currently traveling through.
     */
    private GeoRoute route;

    /**
     * It saves the unsuccessful facts that have happened to the user during the
     * entire simulation.
     */
    private UserMemory memory;

    /**
     * It tries to convince the user to rent or return a bike in a specific
     * station to help balance the system.
     */
    protected RecommendationSystem recommendationSystem;

    /**
     * It informs the user about the state and distance of the different
     * stations.
     */
    protected InformationSystem informationSystem;

    /**
     * It provides the user the availables routes between twoe geographical
     * points.
     */
    protected GraphManager routeService;

    /**
     * It provides facilities of general purpose.
     */
    protected SimulationServices services;

    /**
     * It is the time instant of the simulation.
     */
    private int instant;

    /* random class for generating random events in the user */
    protected SimpleRandom rando;

    protected void readConfigParameters(JsonObject userdef) {
        //get the parameters form the configuration json
        Gson gson = new Gson();
        //necesary paramneters
        JsonElement aux = userdef.get("position");
        if (aux != null) {
            position = gson.fromJson(aux, GeoPoint.class);
        } else {
            throw new IllegalArgumentException("position missing");
        }
        aux = userdef.get("destinationPlace");
        if (aux != null) {
            destinationPlace = gson.fromJson(aux, GeoPoint.class);
        } else {
            throw new IllegalArgumentException("destinationPlace missing");
        }
        aux = userdef.get("timeInstant");
        if (aux != null) {
            instant = aux.getAsInt();
        } else {
            throw new IllegalArgumentException("instant missing");
        }

        //optional parameters
        this.walkingVelocity = 1.12;
        this.cyclingVelocity = 6.0;
        aux = userdef.get("walkingVelocity");
        if (aux != null) {
            walkingVelocity = aux.getAsDouble();
        }
        aux = userdef.get("cyclingVelocity");
        if (aux != null) {
            cyclingVelocity = aux.getAsDouble();
        }
    }

    public User(SimulationServices services, JsonObject userdef, long seed) {

        this.id = idgenerator.next();
        this.rando = new SimpleRandom(seed);

        this.bike = null;
        this.destinationStation = null;
        this.state=STATE.APPEARED;
        this.reservation = null;
        
        //get the parameters form the configuration json
        this.readConfigParameters(userdef);
        this.services = services;
        this.recommendationSystem = services.getRecommendationSystem();
        this.informationSystem = services.getInformationSystem();
        this.routeService = services.getGraphManager(); 
        this.memory = new UserMemory(this);
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public GeoPoint getDestinationPlace() {
        return destinationPlace;
    }

    @Override
    public int getId() {
        return id;
    }

    public void addReservation(Reservation reservation) {
        this.reservation = reservation;
        this.memory.getReservations().add(reservation);
    }

    public Reservation  getReservation() {
        return this.reservation;
    }
    
     public int getInstant() {
        return instant;
    }

    public void setInstant(int instant) {
        this.instant = instant;
    }

    public GeoPoint getPosition() {
        return position;
    }

    public void setPosition(GeoPoint position) {
        this.position = position;
    }

    public Bike getBike() {
        return bike;
    }

    public boolean hasBike() {
        return bike != null ? true : false;
    }

    public boolean hasBikeReservation() {
        if (reservation==null ) return false;
        return reservation.getType()==Reservation.ReservationType.BIKE 
                && reservation.getState()==Reservation.ReservationState.ACTIVE;
    }

    public boolean hasSlotReservation() {
        if (reservation==null ) return false;
        return reservation.getType()==Reservation.ReservationType.SLOT 
                && reservation.getState()==Reservation.ReservationState.ACTIVE;
    }

    public Station getDestinationStation() {
        return destinationStation;
    }

    public GeoRoute getRoute() {
        return this.route;
    }

    public double getWalkingVelocity() {
        return walkingVelocity;
    }

    public double getCyclingVelocity() {
        return cyclingVelocity;
    }

    public UserMemory getMemory() {
        return this.memory;
    }

    /**
     * The user's average velocity in m/s
     *
     * @return user walking velocity if he hasn't a bike at that moment and
     * cycling velocity in other case
     */
    public double getAverageVelocity() {
        return !hasBike() ? walkingVelocity : cyclingVelocity;
    }

    /**
     * User cancels his bike reservation at the specified station.
     *
     * @param station: it is station for which user wants to cancel his bike
     * reservation.
     */
    public void cancelBikeReservationByTimeout(Reservation reservation) {
        if (!this.reservation.equals(reservation)) {
            throw new RuntimeException("invalid program state: cancelBikeReservation");
        }
       reservation = null;
    }

    /**
     * User cancels his slot reservation 
     *
     * @param reservation: it is reservation ti be canceled  
     * reservation.
     */
    public void cancelSlotReservationByTimeout(Reservation reservation) {
       if (!this.reservation.equals(reservation)) {
            throw new RuntimeException("invalid program state: cancelBikeReservation");
        }
        reservation = null;
    }

    /**
     * User tries to remove a bike from specified station.
     *
     * @param station: it is the station where he wnats to remove (rent) a bike.
     * @return true if user has been able to remove a bike (there are available
     * bikes or he has a bike reservation) and false in other case.
     */
    public boolean removeBikeWithoutReservationFrom(Station station) {
        if (hasBike() || hasBikeReservation()) {
            throw new RuntimeException("removeBikeWithoutReservationFrom");
        }
        this.bike = station.removeBikeWithoutReservation();
        return hasBike();
    }

    /**
     * User removes the reserved bike from the specified station.
     *
     * @param station: it is the station where user goes to rent a bike.
     */
    public boolean removeBikeWithReservationFrom(Station station, Reservation res, int instant) {
        if (hasBike() || !hasBikeReservation() || !res.equals(reservation) ) {
            throw new RuntimeException ("invalid program state: user.removeBikeWithReservationFrom");
        }
        this.bike = station.removeBikeWithReservation(reservation, this, instant);
        this.reservation = null;
        if (bike!=null) return true;
        throw new RuntimeException("invalid program flow: removeBikeWithReservationFrom");
    }

    /**
     * User tries to return his rented bike to the specified station.
     *
     * @param station: it is the station where user wants to return his bike.
     * @return true if user has been ablo to return his bike (there available
     * slots or he has a slot reservation) and false in other case.
     */
    public boolean returnBikeWithoutReservationTo(Station station) {
        boolean returned = false;
        if (!hasBike() || hasSlotReservation()) {
            // TODO: log warning (or throw error?)
            throw new RuntimeException("returnBikeWithoutReservationTo");
        }
        if (station.returnBikeWithoutReservation(this.bike)) {
            this.bike = null;
            returned = true;
        }
        return returned;
    }

    /**
     * User returns his bike to specified station.
     *
     * @param station: it is the station at which user arrives in order to
     * return his bike.
     */
    public boolean returnBikeWithReservationTo(Station station, Reservation res, int instant) {
        if (!hasBike() || !hasSlotReservation() || !res.equals(reservation) || !station.returnBikeWithReservation(this.bike, reservation, this, instant) ) {
            throw new RuntimeException ("invalid program state: user.returnBikeWithReservationTo");
        }
        this.bike=null;
        this.reservation = null;
        return true;
   }

    private GeoRoute calculateRoute(GeoPoint destinationPoint) throws GeoRouteCreationException, GraphHopperIntegrationException {
        String vehicle = this.bike == null ? "foot" : "bike";

        if (this.position.equals(destinationPoint)) {
            List<GeoPoint> patchedRoute = new ArrayList<>(Arrays.asList(this.position, destinationPoint));
            return new GeoRoute(patchedRoute);
        }
        try {
            return routeService.obtainShortestRouteBetween(this.position, destinationPoint, vehicle);
        } catch (Exception e) {
            List<GeoPoint> patchedRoute = new ArrayList<>(Arrays.asList(this.position, destinationPoint));
            return new GeoRoute(patchedRoute);
        }

    }

    /**
     * Time in seconds that user takes in arriving to a GeoPoint time =
     * distance/velocity
     *
     * @throws Exception
     */
    protected int timeToReach() {
        return (int) (route.getTotalDistance() / getAverageVelocity());
    }

    public GeoPoint reachedPointUntilTimeOut() throws GeoRouteException, GeoRouteCreationException {
        return route.calculatePositionByTimeAndVelocity(Reservation.VALID_TIME, this.getAverageVelocity());
    }

    /**
     * When user is going to a station and timeout happens, it calculates how
     * far he has gotten in order to update his position. This position is
     * currently at the last position of the current route
     */
    public void leaveSystem() {
        setPosition(null);
        route = null;
        destinationStation = null;
        instant = -1;
    }

    /**
     * The user walks to a station; sets the route and the destination station.
     * devuelve el tiempo que tardará el usario
     */
    final public int goToStation(Station dest) throws Exception {
        destinationStation = dest;
        route = calculateRoute(dest.getPosition());
        return (int) (route.getTotalDistance() / getAverageVelocity());
    }

    /**
     * The user goes to e point in the city not a station. devuelve el tiempo
     * que tardará el usario
     */
    public int goToPointInCity(GeoPoint point) throws Exception {
        destinationStation = null;
        route = calculateRoute(point);
        return (int) (route.getTotalDistance() / getAverageVelocity());
    }

    /*****************************************************************
    ** USER DECISION FUNCTIONS, to be implemented by specific users
    *****************************************************************/
            
    /**
     * User decides what to do after appearing. possible outcomes is UserDecisionS¡tation or UserDecisionLeafSystem
     *
     * @return a user decision.
     */
    public abstract UserDecision decideAfterAppearning();
    
     /**
     * User decides what to do after beeing unable to take a bike at the station he arrived.
     * possible outcomes is UserDecisionS¡tation with or without reservation or UserDecisionLeafSystem
     *
     * @return a user decision.
     */
    public abstract UserDecision decideAfterFailedRental();

    /**
     * User decides what to do after a failed bike Reservation.
     * possible outcomes is UserDecisionS¡tation or UserDecisionLeafSystem
     *
     * @return a user decision.
     */
    public abstract UserDecision decideAfterFailedBikeReservation();
        
    /**
     * User decides what to do after a reservation timeout for a rental has taken place  .
     * possible outcomes is UserDecisionS¡tation with or without reservation or UserDecisionLeafSystem
     *
     * @return a user decision.
     */
    public abstract UserDecision decideAfterBikeReservationTimeout();

    /**
     * User decides what to do after a
     * getting a bike.
     *Possible outcomes are UserDecisionGoToPointInCity or UserDecisionStation with or without reservation
     * @return a user decision. possible outcomes is UserDecisionS¡tation
     */
    public abstract UserDecision decideAfterGettingBike();
    
    /**
     * User decides what to do after a failed return intent   .
     * possible outcomes is UserDecisionStation with or without reservation of a slot
     *
     * @return a user decision.
     */
    public abstract UserDecisionStation decideAfterFailedReturn();

     /**
     * User decides what to do after a
     * finishing a biuke ride to a point in the city .
     *Possible outcomes are UserDecisionStation with or without reservation of a slot
     * @return a user decision. 
     */
    public abstract UserDecisionStation decideAfterFinishingRide();

    /**
     * User decides what to do after a failed Slot Reservation.
     *
     * @return a user decision. possible outcomes is UserDecisionS¡tation
     */
    public abstract UserDecisionStation decideAfterFailedSlotReservation();
    
   /**
     * User decides what to do after a Slot Reservation timeout.
     *
     * @return a user decision. possible outcomes is UserDecisionS¡tation
     */
    public abstract UserDecisionStation decideAfterSlotReservationTimeout();

    /********************************************
     * Methods for getting stations
     * 
     */
       /**
     * It randomly chooses a station among the pre-established number of nearest
     * stations.
     */
    protected abstract Station determineStationToRentBike() ;
    protected abstract Station determineStationToReturnBike() ;
    
    
    @Override
    public String toString() {
        String result = this.getClass().getSimpleName()+" : | Id: " + getId();
        result += " | State: " + state;
        if (position != null) {
            result += "| Actual Position: " + position.toString();
        } else {
            result += "| Actual Position: null";
        }
        result += " | Has Bike: " + hasBike();
        result += " | Actual velocity: " + getAverageVelocity();
        result += "| Has reserved bike: " + hasBikeReservation();
        result += " | Has reserved slot: " + hasSlotReservation() ;
        if (destinationStation != null) {
            result += "| Destination station: " + destinationStation.getId();
        } else {
            result += "| Destination station: " + null;
        }

        return result;
    }
}
