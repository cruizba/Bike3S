package es.urjc.ia.bikesurbanfleets.worldentities.infraestructure;

import es.urjc.ia.bikesurbanfleets.common.demand.DemandManager;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Bike;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * This class contains all the information of all the entities at the system.
 * It provides all the usable methods by the user at the system.
 * @author IAgroup
 */
public class InfraestructureManager {

    /**
     * These are all the stations at the system.
     */
    private List<Station> stations;
    
    /**
     * These are all the bikes from all stations at the system.
     */
    private List<Bike> bikes;
        
    private int maxStationCapacity;
    private int minStationCapacity;
    
    public int getMaxStationCapacity() {
        return maxStationCapacity;
    }

    public int getMinStationCapacity() {
        return minStationCapacity;
    }
    
    public InfraestructureManager(List<Station> stations) throws IOException {
        this.stations = stations;
        this.bikes = stations.stream().map(Station::getSlots).flatMap(List::stream).filter(Objects::nonNull).collect(Collectors.toList());
        OptionalInt i =stations.stream().mapToInt(Station::getCapacity).max();
        if (!i.isPresent()) throw new RuntimeException("invalid program state: no stations");
        maxStationCapacity=i.getAsInt();
        i =stations.stream().mapToInt(Station::getCapacity).min();
        if (!i.isPresent()) throw new RuntimeException("invalid program state: no stations");
        minStationCapacity=i.getAsInt();      
    }
    

    public List<Station> consultStations() {
        return stations;
    } 
    
    public List<Bike> consultBikes() {
    	return this.bikes;
    }    

 }
