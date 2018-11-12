package es.urjc.ia.bikesurbanfleets.core.events;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoRoute;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Entity;
import es.urjc.ia.bikesurbanfleets.users.User;
import es.urjc.ia.bikesurbanfleets.users.UserMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventUserArrivesAtStationToReturnBikeWithoutReservation extends EventUser {
    private List<Entity> entities;
    private Station station;

    public EventUserArrivesAtStationToReturnBikeWithoutReservation(int instant, User user, Station station) {
        super(instant, user);
        this.entities = new ArrayList<>(Arrays.asList(user, station));
        this.station = station;
    }
    
    public Station getStation() {
        return station;
    }

    @Override
    public List<Event> execute() throws Exception {
        List<Event> newEvents = new ArrayList<>();
        user.setInstant(this.instant);
        user.setPosition(station.getPosition());
        user.setState(User.STATE.WITH_BIKE);
        debugEventLog();
        if(!user.returnBikeWithoutReservationTo(station)) {
            user.getMemory().update(UserMemory.FactType.SLOTS_UNAVAILABLE);
            debugEventLog("User can't return bike. Station info: " + station.toString()) ;
            newEvents = manageSlotReservationDecisionAtOtherStation();
        } else {
            GeoPoint point = user.getDestinationPlace();
            int arrivalTime = user.goToPointInCity(point);
            user.setState(User.STATE.WALK_TO_DESTINATION);
            debugEventLog("User returns the bike without reservation. Destination in city: " + point.toString());
            newEvents.add(new EventUserArrivesAtDestinationInCity(this.instant + arrivalTime, user, point));
        }
        return newEvents;
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }
}
