package es.urjc.ia.bikesurbanfleets.infraestructure.entities;

import es.urjc.ia.bikesurbanfleets.common.interfaces.Entity;
import es.urjc.ia.bikesurbanfleets.common.util.IdGenerator;
import es.urjc.ia.bikesurbanfleets.history.entities.HistoricReservation;
import es.urjc.ia.bikesurbanfleets.users.User;
import es.urjc.ia.bikesurbanfleets.history.History;
import es.urjc.ia.bikesurbanfleets.history.HistoryReference;

/**
 * It can be a bike or slot reservation depending on its type property
 * It can represents a reservations at all its possible states:
 *      ACTIVE: the reservation is valid at that moment
 *   FAILED: user has tried to make a reservation and it hasn't been possible (there're no available bikes or solts)
 *   EXPIRED: reservation has been made but timeout has happend
 *   SUCCESSFUL: user has removed or returned his bike, so reservation has been resolved succesfully (the reservation ceases to exist)
 * @author IAgroup
 *
 */

@HistoryReference(HistoricReservation.class)
public class Reservation implements Entity {

    public enum ReservationType {
        SLOT, BIKE
    }

    public enum ReservationState {
        ACTIVE, FAILED, EXPIRED, SUCCESSFUL
    }

    /**
     * It is the time during which a reservation is active.
     */
    public static int VALID_TIME;

    private int id;
    private int startInstant;  // instant when user makes the reservation
    private int endInstant;  // instant when reservation is resolved or expired
    private ReservationType type;
    private ReservationState state;
    private User user;
    private Station station;
    
    public static IdGenerator idgenerator;
    
    public static void resetIdGenerator(){
        idgenerator=new IdGenerator();
    }
    /**
     * It is the bike which the user reserves or the rented bike which the user wants to return.
     */
    private Bike bike;

    /**
     * As it receives a bike param, it creates an active reservation
     */

    public Reservation(int startInstant, ReservationType type, User user, Station station, Bike bike) {
        this.id = idgenerator.next();
        this.startInstant = startInstant;
        this.endInstant = -1; // reservation has'nt ended
        this.type = type;
        this.state = ReservationState.ACTIVE;
        this.user = user;
        this.station = station;
        this.bike = bike;
        History.registerEntity(this);
    }

    /**
     * As it doesn't receive a bike parameter, it creates a failed reservation
     */
    public Reservation(int startInstant, ReservationType type, User user, Station station) {
        this.id = idgenerator.next();
        this.startInstant = startInstant;
        this.endInstant = startInstant;
        this.type = type;
        this.state = ReservationState.FAILED;
        this.user = user;
        this.station = station;
        this.bike = null;
        History.registerEntity(this);
    }

    @Override
    public int getId() {
        return id;
    }

    public int getStartInstant() {
        return startInstant;
    }

    public int getEndInstant() {
        return endInstant;
    }

    public ReservationType getType() {
        return type;
    }

    public ReservationState getState() {
        return state;
    }

    public User getUser() {
        return user;
    }

    public Station getStation() {
        return station;
    }
    
    public Bike getBike() {
        return bike;
    }
    
        /**
     * Set reservation state to expired and updates reservation end instant
     */
    public void expire() {
        this.state = ReservationState.EXPIRED;
        this.endInstant = this.startInstant + VALID_TIME;
    }

    /**
     * Set reservation state to successful and updates reservation end instant
     * @param endInstant: it is the time instant when user removes or returns a bike with a previous bike or slot reservation, respectively
     */
    public void resolve(int endInstant) {
        this.state = ReservationState.SUCCESSFUL;
        this.endInstant = endInstant;
    }
}