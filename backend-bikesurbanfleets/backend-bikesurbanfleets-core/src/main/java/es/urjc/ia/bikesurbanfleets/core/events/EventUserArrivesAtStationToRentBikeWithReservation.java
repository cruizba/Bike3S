package es.urjc.ia.bikesurbanfleets.core.events;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoRoute;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Entity;
import es.urjc.ia.bikesurbanfleets.users.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventUserArrivesAtStationToRentBikeWithReservation extends EventUser {

    private List<Entity> entities;
    private Station station;
    private Reservation reservation;

    public EventUserArrivesAtStationToRentBikeWithReservation(int instant, User user, Station station, Reservation reservation) {
        super(instant, user);
        this.entities = new ArrayList<>(Arrays.asList(user, station, reservation));
        this.station = station;
        this.reservation = reservation;
    }
    
    public Station getStation() {
        return station;
    }

    public Reservation getReservation() {
        return reservation;
    }

    @Override
    public List<Event> execute() throws Exception {
        List<Event> newEvents = new ArrayList<>();
        user.setInstant(this.instant);
        user.setPosition(station.getPosition());
        user.setState(User.STATE.WITH_BIKE);
        reservation.resolve(instant);
        user.removeBikeWithReservationFrom(station);
        debugEventLog("User removes Bike with reservation");
        if (!user.decidesToGoToPointInCity()) {  // user goes directly to another station to return his bike
            debugEventLog("User decides to return bike to other station");
            newEvents = manageSlotReservationDecisionAtOtherStation();
        } else {   // user rides his bike to a point which is not a station
            GeoPoint point = user.getPointInCity();
            int arrivalTime = user.goToPointInCity(point);
            debugEventLog("User decides to take a ride");
            newEvents.add(new EventUserWantsToReturnBike(getInstant() + arrivalTime, user, point));
        }
        return newEvents;
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }
}
