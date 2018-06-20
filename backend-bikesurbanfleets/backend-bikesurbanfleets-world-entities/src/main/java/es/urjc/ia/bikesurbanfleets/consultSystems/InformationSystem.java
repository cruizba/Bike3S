package es.urjc.ia.bikesurbanfleets.consultSystems;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.infraestructure.InfraestructureManager;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;

import java.util.List;
import java.util.stream.Collectors;

/** 
 * @author IAgroup
 *
 */
public class InformationSystem {

	private InfraestructureManager infraestructureManager;
	private StationComparator stationComparator;
	
    public InformationSystem(InfraestructureManager infraestructureManager, StationComparator stationComparator) {
    	this.infraestructureManager = infraestructureManager;
    	this.stationComparator = stationComparator;
    }
    
    private List<Station> validStationInfosToRentBike(GeoPoint point, int maxDistance, List<Station> stations) {
    	return stations.stream().filter(station -> station.getPosition()
            .distanceTo(point) <= maxDistance && station.availableBikes() > 0)
            .collect(Collectors.toList());
    }
    
    private List<Station> validStationInfosToRentBike(List<Station> stations) {
    	return stations.stream().filter(station -> station.availableBikes() > 0)
           .collect(Collectors.toList());
    }

    private List<Station> validStationInfosToReturnBike(List<Station> stations) {
    	return stations.stream().filter( station ->	station.availableSlots() > 0)
            .collect(Collectors.toList());
    }

    /**
     * It recommends stations by the nunmber of available bikes they have: first, it recommends 
     * those which have the most bikes available and finally, those with the least bikes available.
     * @param point It's the user current position or the geographical coordinates of a 
     * place the user wants to reach.
     * @return a list of stations ordered descending by the number of available bikes.
     */
    public List<Station> getStationsOrderedByNumberOfBikes(GeoPoint point, int maxDistance) {
    	List<Station> stations = infraestructureManager.consultStations();
        return validStationInfosToRentBike(point, maxDistance, stations).stream().sorted(stationComparator.byAvailableBikes()).collect(Collectors.toList());
    }
    
    public List<Station> getStationsOrderedByNumberOfBikes() {
    	List<Station> stations = infraestructureManager.consultStations();
        return validStationInfosToRentBike(stations).stream().sorted(stationComparator.byAvailableBikes()).collect(Collectors.toList());
    }
    
    /**
     * It recommends stations by the nunmber of available slots they have: first, it recommends 
     * those which have the most slots available and finally, those with the least slots available.
     * @return a list of stations ordered descending by the number of available slots.
     */
    public List<Station> getStationsOrderedByNumberOfSlots() {
    	List<Station> stations = infraestructureManager.consultStations();
        return validStationInfosToReturnBike(stations).stream().sorted(stationComparator.byAvailableSlots()).collect(Collectors.toList());
    }
    
    /**
     * It recommends stations by a factor which consists of the quotient between the distance 
     * from each station to the specified geographical point and the number of available bikes 
     * the station contains: first, it recommends those stations which have the smallest proportion 
     * and finally, those with the greatest one (the smallest the quotient, the better the station).
     * @param point It's the user current position or the geographical coordinates of a 
     * place the user wants to reach.
     * @return a list of stations ordered asscending by the previously described proportion.  
     */
    public List<Station> recommendByProportionBetweenDistanceAndBikes(GeoPoint point, int maxDistance) {
    	List<Station> stations = infraestructureManager.consultStations();
        return validStationInfosToRentBike(point, maxDistance, stations)
            .stream().sorted(stationComparator.byProportionBetweenDistanceAndBikes(point)).collect(Collectors.toList());
    }
    
    public List<Station> recommendByProportionBetweenDistanceAndBikes(GeoPoint point) {
    	List<Station> stations = infraestructureManager.consultStations();
        return validStationInfosToRentBike(stations)
            .stream().sorted(stationComparator.byProportionBetweenDistanceAndBikes(point)).collect(Collectors.toList());
        }
    
    /**
     * It recommends stations by a factor which consists of the quotient between the distance 
     * from each station to the specified geographical point and the number of available slots
     * the station contains: first, it recommends those stations which have the smallest proportion 
     * and finally, those with the greatest one (the smallest the quotient, the better the station).
     * @param point It's the user current position or the geographical coordinates of a 
     * place the user wants to reach.
     * @return a list of stations ordered asscending by the previously described proportion.  
     */
    public List<Station> recommendByProportionBetweenDistanceAndSlots(GeoPoint point) {
    	List<Station> stations = infraestructureManager.consultStations();
    	return validStationInfosToReturnBike(stations)
          		.stream().sorted(stationComparator.byProportionBetweenDistanceAndSlots(point)).collect(Collectors.toList());
    }
    
    /**
     * It recommends stations by the distance (linear or real depending on a global configuration 
     * parameter) they are from the specified geographical point: first, it recommends 
     * those which are closest to the point and finally, those wich are the most 
     * distant to taht same point.
     * @param point It's the user current position or the geographical coordinates of a 
     * place the user wants to reach.
     * @return a list of stations ordered asscending by the linear distance from them to 
     * the specified geographical point.
     */
    public List<Station> recommendToRentBikeByDistance(GeoPoint point, int maxDistance) {
    	List<Station> stations = infraestructureManager.consultStations();
     return validStationInfosToRentBike(point, maxDistance, stations)
        		.stream().sorted(stationComparator.byDistance(point)).collect(Collectors.toList());
    }
    
    public List<Station> recommendToRentBikeByDistance(GeoPoint point) {
    	List<Station> stations = infraestructureManager.consultStations();
     return validStationInfosToRentBike(stations)
        		.stream().sorted(stationComparator.byDistance(point)).collect(Collectors.toList());
    }

    public List<Station> recommendToReturnBikeByDistance(GeoPoint point) {
    	List<Station> stations = infraestructureManager.consultStations();
     return validStationInfosToReturnBike(stations)
        		.stream().sorted(stationComparator.byDistance(point)).collect(Collectors.toList());
    }
    
    
}