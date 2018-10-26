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
import es.urjc.ia.bikesurbanfleets.history.entities.HistoricUser;
import es.urjc.ia.bikesurbanfleets.infraestructure.InfraestructureManager;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Bike;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;
import es.urjc.ia.bikesurbanfleets.history.History;
import es.urjc.ia.bikesurbanfleets.history.HistoryReference;
import java.lang.reflect.Field;

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
@HistoryReference(HistoricUser.class)
public abstract class User implements Entity {

    public enum STATE {
        APPEARED, WALK_TO_STATION, WITH_BIKE, WALK_TO_DESTINATION, EXIT_AFTER_TIMEOUT,
        EXIT_AFTER_FAILED_RESERVATION,EXIT_AFTER_FAILED_RENTAL, EXIT_AFTER_REACHING_DESTINATION, LEFT_SYSTEM 
    }
    
    private static IdGenerator idGenerator = new IdGenerator();

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
     * It indicates if user has a reserved bike currently.
     */
    private boolean reservedBike;

    /**
     * It indicates if user has a reserved slot currently.
     */
    private boolean reservedSlot;

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

    protected InfraestructureManager infraestructure;

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
    protected GraphManager graph;

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

        this.id = idGenerator.next();
        this.rando = new SimpleRandom(seed);

        this.bike = null;
        this.reservedBike = false;
        this.reservedSlot = false;
        this.destinationStation = null;
        this.state=STATE.APPEARED;
        this.reservation = null;
        this.memory = new UserMemory(this);

        // ******* Historic treatment *******
        // it's necessary to register the user here, to detect changes 
        // in the event execution
        History.registerEntity(this);

        //first get the parameters form the configuration json
        this.readConfigParameters(userdef);
        this.services = services;
        this.infraestructure = services.getInfrastructureManager();
        this.recommendationSystem = services.getRecommendationSystem();
        this.informationSystem = services.getInformationSystem();
        this.graph = services.getGraphManager(); 
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
        infraestructure.addReservation(reservation);
        this.reservation = reservation;
        this.memory.getReservations().add(reservation);
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

    public boolean hasReservedBike() {
        return reservedBike;
    }

    public boolean hasReservedSlot() {
        return reservedSlot;
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
     * User tries to reserve a bike at the specified station.
     *
     * @param station: it is the station for which user wants to make a bike
     * reservation.
     * @return the reserved bike if user has been able to reserve one at that
     * station (there're available bikes) and false in other case.
     */
    public Bike reservesBike(Station station) {
        Bike bike = null;
        if (station.availableBikes() > 0) {
            this.reservedBike = true;
            bike = station.reservesBike();
        }
        return bike;
    }

    /**
     * User tries to reserve a slot at the specified station.
     *
     * @param station: it is the station for which user wants to make a slot
     * reservation.
     * @return true if user has been able to reserve a slot at that station
     * (there're available slots) and false in other case.
     */
    public boolean reservesSlot(Station station) {
        if (station.availableSlots() > 0) {
            this.reservedSlot = true;
            station.reservesSlot();
        }
        return reservedSlot;
    }

    /**
     * User cancels his bike reservation at the specified station.
     *
     * @param station: it is station for which user wants to cancel his bike
     * reservation.
     */
    public void cancelsBikeReservation(Station station) {
        this.reservedBike = false;
        station.cancelsBikeReservation(reservation);
    }

    /**
     * User cancels his slot reservation at the specified station.
     *
     * @param station: it is station for which user wants to cancel his slot
     * reservation.
     */
    public void cancelsSlotReservation(Station station) {
        this.reservedSlot = false;
        station.cancelsSlotReservation();
    }

    /**
     * User tries to remove a bike from specified station.
     *
     * @param station: it is the station where he wnats to remove (rent) a bike.
     * @return true if user has been able to remove a bike (there are available
     * bikes or he has a bike reservation) and false in other case.
     */
    public boolean removeBikeWithoutReservationFrom(Station station) {
        if (hasBike()) {
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
    public void removeBikeWithReservationFrom(Station station) {
        if (hasReservedBike()) {
            // first, reservation is cancelled to let a bike available at station to make sure one bike is available for take away
            cancelsBikeReservation(station);
        }
        this.bike = station.removeBikeWithReservation(reservation);
        this.reservation = null;
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
        if (!hasBike()) {
            // TODO: log warning (or throw error?)
            throw new RuntimeException("returnBikeWithoutReservationTo");
        }
        if (station.returnBike(this.bike)) {
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
    public void returnBikeWithReservationTo(Station station) {
        if (hasReservedSlot()) {
            cancelsSlotReservation(station);
        }
        if (station.returnBike(this.bike)) {
            this.bike = null;
        }
        this.reservation = null;
    }

    private GeoRoute calculateRoute(GeoPoint destinationPoint) throws GeoRouteCreationException, GraphHopperIntegrationException {
        String vehicle = this.bike == null ? "foot" : "bike";

        if (this.position.equals(destinationPoint)) {
            List<GeoPoint> patchedRoute = new ArrayList<>(Arrays.asList(this.position, destinationPoint));
            return new GeoRoute(patchedRoute);
        }
        try {
            return graph.obtainShortestRouteBetween(this.position, destinationPoint, vehicle);
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

    /**
     * User decides if he'll leave the system when bike reservation timeout
     * happens.
     *
     * @return true if he decides to leave the system and false in other case
     * (he decides to continue at system).
     */
    public abstract boolean decidesToLeaveSystemAfterTimeout();

    /**
     * User decides if he'll leave the system after not being able to make a
     * bike reservation.
     *
     * @return true if he decides to leave the system and false in other case
     * (he decides to continue at system).
     */
    public abstract boolean decidesToLeaveSystemAffterFailedReservation();

    /**
     * User decides if he'll leave the system when there're no avalable bikes at
     * station.
     *
     * @return true if he decides to leave the system and false in other case
     * (he decides to continue at system).
     */
    public abstract boolean decidesToLeaveSystemWhenBikesUnavailable();

    /**
     * User decides to which station he wants to go to rent a bike.
     *
     * @return station where user has decided to go.
     */
    public abstract Station determineStationToRentBike();

    /**
     * User decides to which station he wants to go to return his bike.
     *
     * @return station where user has decided to go.
     */
    public abstract Station determineStationToReturnBike();

    /**
     * User decides if he'll try to make again a bike reservation at the
     * previosly chosen station after timeout happens.
     *
     * @return true if user decides to reserve a bike at the initially chosen
     * station.
     */
    public abstract boolean decidesToReserveBikeAtSameStationAfterTimeout();

    /**
     * User decides if he'll try to make a bike reservation at a new chosen
     * station.
     *
     * @return true if user decides to reserve a bike at that new station and
     * false in other case.
     */
    public abstract boolean decidesToReserveBikeAtNewDecidedStation();

    /**
     * User decides if he'll try to make again a slot reservation at the
     * previosly chosen station after timeout happens.
     *
     * @return true if user decides to reserve a slot at the initially chosen
     * station.
     */
    public abstract boolean decidesToReserveSlotAtSameStationAfterTimeout();

    /**
     * User decides if he'll try to make a slot reservation at a new chosen
     * station.
     *
     * @return true if user decides to reserve a slot at that new station and
     * false in other case.
     */
    public abstract boolean decidesToReserveSlotAtNewDecidedStation();

    /**
     * Just after removing the bike, user decides if he'll ride it directly to a
     * station or he will go to a point in the city in order to return it.
     *
     * @return true if user decides to ride to a point in the city and false in
     * other case (he decides to go to a station close).
     */
    public abstract boolean decidesToGoToPointInCity();

    /**
     * If the user decides thogo to a point in the city, this is that point,
     * that is if decidesToGoToPointInCity is true
     *
     * @return the point where he wants to go after making his decision.
     */
    public abstract GeoPoint getPointInCity();

    /**
     * When timeout happens, he decides to continue going to that chosen station
     * or to go to another one.
     *
     * @return true if user chooses a new station to go and false if he
     * continues to the previously chosen one.
     */
    public abstract boolean decidesToDetermineOtherStationAfterTimeout();

    /**
     * When user hasn't been able to make a reservation at the destination
     * station, he decides if he wants to choose another station to which go.
     *
     * @return true if he decides to determine another destination station and
     * false in other case (he keeps his previously decision).
     */
    public abstract boolean decidesToDetermineOtherStationAfterFailedReservation();

    @Override
    public String toString() {
        String result = "| Id: " + getId();
        if (position != null) {
            result += "| Actual Position: " + position.toString();
        } else {
            result += "| Actual Position: null";
        }
        result += " | Has Bike: " + hasBike();
        result += " | Actual velocity: " + getAverageVelocity();
        result += "| Has reserved bike: " + hasReservedBike();
        result += " | Has reserved slot: " + hasReservedSlot() + "\n";
        if (destinationStation != null) {
            result += "| Destination station: " + destinationStation.getId();
        } else {
            result += "| Destination station: " + null;
        }

        return result;
    }
}
