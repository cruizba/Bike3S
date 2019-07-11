package es.urjc.ia.bikesurbanfleets.history.entities;

import com.google.gson.annotations.Expose;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;

import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Reservation;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;
import es.urjc.ia.bikesurbanfleets.history.JsonIdentifier;

import java.util.stream.Collectors;

/**
 * It contains the rlevant information of a specific station, e. g., its history.
 * @author IAgroup
 *
 */
@JsonIdentifier("stations")
public class HistoricStation implements HistoricEntity {

    @Expose
    private int id;
    
    @Expose
    private int oficialID;

    @Expose
    private GeoPoint position;

    @Expose
    private int capacity;

    @Expose
    private int availablebikes;

    @Expose
    private int reservedbikes;

    @Expose
    private int reservedslots;
   
    @Expose
    private int availableslots;

   // @Expose
   // private IdReference bikes;

   // @Expose
   // private IdReference reservations;

    public HistoricStation(Station station) {
        this.id = station.getId();
        this.oficialID=station.getId();
        this.position = new GeoPoint(station.getPosition());
        this.capacity = station.getCapacity();
    /*    this.bikes = new IdReference(
                HistoricBike.class,
                station.getBikes().stream()
                        .map(bike -> bike == null ? null : bike.getId())
                        .collect(Collectors.toList())
        ); */
        this.availablebikes=station.availableBikes();
        this.reservedbikes=station.getReservedBikes();
        this.availableslots=station.availableSlots();
        this.reservedslots=station.getReservedSlots();

    /*    this.reservations = new IdReference(
                HistoricReservation.class,
                station.getReservations().values().stream()
                        .map(Reservation::getId)
                        .collect(Collectors.toList())
        );*/
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailablebikes() {
        return availablebikes;
    }

    @Override
    public int getId() {
        return id;
    }
}
