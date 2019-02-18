package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems;

import es.urjc.ia.bikesurbanfleets.common.demand.DemandManager;
import java.util.List;
import java.util.stream.Collectors;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes.Recommendation;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.InfrastructureManager;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;
import java.util.ArrayList;
import java.util.Comparator;

public abstract class RecommendationSystem {

    /**
     * It provides information about the infraestructure state.
     */
    protected InfrastructureManager infrastructureManager;
    
    /**
     * It filters stations which have not available bikes.
     *
     * @return a list of stations with available bikes.
     */
    protected static List<Station> validStationsToRentBike(List<Station> stations) {
        return stations.stream().filter(station -> station.availableBikes() > 0)
                .collect(Collectors.toList());
    }

    /**
     * It filters stations which have not available bikes.
     *
     * @return a list of stations with available bikes.
     */
    protected static List<Station> validStationsToReturnBike(List<Station> stations) {
        return stations.stream().filter((station) -> station.availableSlots() > 0)
                .collect(Collectors.toList());
    }

    public RecommendationSystem(SimulationServices simulationServices) {
        this.infrastructureManager = simulationServices.getInfrastructureManager();
    }

    protected abstract List<Recommendation> recommendStationToRentBike(GeoPoint point);

    protected abstract List<Recommendation> recommendStationToReturnBike(GeoPoint point);

    public List<Recommendation> getRecomendedStationToRentBike(GeoPoint point){
        return addAlternativeRecomendations(point,recommendStationToRentBike(point), true);
     }
    public List<Recommendation> getRecomendedStationToReturnBike(GeoPoint point){
        return addAlternativeRecomendations(point,recommendStationToReturnBike(point), false);
     }
       
    private boolean containsStation(List<Recommendation> recs, Station s){
        if (recs.stream().anyMatch((r) -> (r.getStation()==s))) {
            return true;
        }
        return false;
    }
    
    private  List<Recommendation> addAlternativeRecomendations(GeoPoint point, List<Recommendation> recs, boolean take){
        if (recs==null) recs=new ArrayList<>(); 
        int numrecsrequired=100-recs.size();
        if (numrecsrequired>0){
            int i=0;
            Comparator<Station> byDistance = StationComparator.byDistance(point);
            List<Station> temp1;
            if (take){
                temp1 = validStationsToRentBike(infrastructureManager.consultStations());
            } else {
                temp1 = validStationsToReturnBike(infrastructureManager.consultStations());           
            }
            List<Station> temp = temp1.stream().sorted(byDistance).collect(Collectors.toList());
            while (numrecsrequired>0 && i<temp.size()){
                Station s=temp.get(i);
                if (!containsStation(recs,s)){
                    recs.add(new Recommendation(s, null));
                    numrecsrequired--;
                }
                i++;
            }
        }
        return recs;
    }

    //auxiliary function to normalize values in a linear way to the range [0,1]
    protected double normatizeTo01(double value, double minvalue, double maxvalue){
        if (maxvalue<=minvalue) throw new RuntimeException("invalid program state");
        if (value<minvalue) throw new RuntimeException("invalid program state");
        if (value>maxvalue) throw new RuntimeException("invalid program state");
        return (value-minvalue)/(maxvalue-minvalue);
    }
    
}
