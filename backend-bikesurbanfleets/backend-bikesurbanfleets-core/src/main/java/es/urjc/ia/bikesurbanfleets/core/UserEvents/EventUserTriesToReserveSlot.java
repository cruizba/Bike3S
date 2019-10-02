/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.core.UserEvents;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;
import es.urjc.ia.bikesurbanfleets.worldentities.users.User;
import es.urjc.ia.bikesurbanfleets.worldentities.users.UserMemory;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author holger
 */
public class EventUserTriesToReserveSlot extends EventUser {

    Station station;
    private boolean waitingEvent=false;

    public EventUserTriesToReserveSlot(int instant, User user, Station station, boolean wait) {
        super(instant, user);
        this.involvedEntities = new ArrayList<>(Arrays.asList(user, station));
        this.newEntities = null;
        this.oldEntities = null;
        this.station = station;
        waitingEvent=wait;
    }

    @Override
    public EventUser execute() throws Exception {
        debugEventLog("At enter the event");

        Reservation reservation = station.getSlotReservation(user, this.instant);
        this.involvedEntities.add(reservation);
        this.newEntities = new ArrayList<>(Arrays.asList(reservation));

        //set the result of the event
        //the result of EventUserTriesToReserveSlot is either SUCCESS or FAIL
        ADDITIONAL_INFO info=null;
        if(waitingEvent) info=ADDITIONAL_INFO.RETRY_EVENT;
        if (reservation.getState() == Reservation.ReservationState.ACTIVE) setResultInfo(Event.RESULT_TYPE.SUCCESS, info);
        else setResultInfo(Event.RESULT_TYPE.FAIL, info);
   
        //now decide what to do afterwards
        EventUser e;
        if (reservation.getState() == Reservation.ReservationState.ACTIVE) {   // user has been able to reserve a slot
            user.addReservation(reservation);
            e = manageFactsAfterSlotReservation(reservation);
        } else {  // user has notbeen able to reserve a slot
            this.oldEntities = new ArrayList<>(Arrays.asList(reservation));
            user.getMemory().update(UserMemory.FactType.SLOT_FAILED_RESERVATION,station);
            debugEventLog("User has not been able to reserve slot");
            e = manageUserReturnDecision(DECISION_TYPE.AFTER_FAILED_SLOT_RESERVATION);
        }
        
        return e;
    }

    /**
     * if the reservation has been sucessful, the user goes towards the sation
 and there are two possibilities: EventUserSlotReservationTimeout or
 EventUserArrivesAtStationToReturnBike(with reservation)
     */
    private EventUser manageFactsAfterSlotReservation(Reservation reservation) throws Exception {
        int arrivalTime = user.goToStation(station);
        user.setState(User.STATE.WITH_BIKE_TO_STATION);
        debugEventLog("User has been able to reserve slot and goes with bike to the station");
        if (Reservation.VALID_TIME < arrivalTime) {
            GeoPoint pointTimeOut = user.reachedPointUntilTimeOut();
            return new EventUserSlotReservationTimeout(this.getInstant() + Reservation.VALID_TIME, user, reservation, station, pointTimeOut);
        } else {
            return new EventUserArrivesAtStationToReturnBike(this.getInstant() + arrivalTime, user, station, reservation, false);
        }
    }
}
