package es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.simple;

import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.Recommendation;
import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.StationManager;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is a system which recommends the user the stations to which he
 * should go to contribute with system rebalancing. Then, this recommendation
 * system gives the user a list of stations ordered descending by the
 * "resources/capacityº" ratio.
 *
 * @author IAgroup
 *
 */
@RecommendationSystemType("RESOURCES_RATIO")
public class RecommendationSystemByResourcesRatio extends RecommendationSystem {

    public class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendation = 600;
        @Override
        public String toString() {
            return "maxDistanceRecommendation=" + maxDistanceRecommendation ;
        }
    }
    public String getParameterString(){
        return "RecommendationSystemByResourcesRatio Parameters{"+ this.parameters.toString() + "}";
    }

    private RecommendationParameters parameters;

    public RecommendationSystemByResourcesRatio(JsonObject recomenderdef, SimulationServices ss) throws Exception {
        super(ss);
        //***********Parameter treatment*****************************
        //if this recomender has parameters this is the right declaration
        //if no parameters are used this code just has to be commented
        //"getparameters" is defined in USER such that a value of Parameters 
        // is overwritten if there is a values specified in the jason description of the recomender
        // if no value is specified in jason, then the orriginal value of that field is mantained
        // that means that teh paramerts are all optional
        // if you want another behaviour, then you should overwrite getParameters in this calss
        this.parameters = new RecommendationParameters();
         getParameters(recomenderdef, this.parameters);
  }

    @Override
    public List<Recommendation> recommendStationToRentBike(GeoPoint point) {
        List<Station> temp;
        List<Recommendation> result = new ArrayList<>();
        List<Station> stations = validStationsToRentBike(stationManager.consultStations()).stream()
                .filter(station -> station.getPosition().distanceTo(point) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            Comparator<Station> byBikesRatio = byBikesCapacityRatio(point);
            temp = stations.stream().sorted(byBikesRatio).collect(Collectors.toList());
            result = temp.stream().map(station -> new Recommendation(station, null)).collect(Collectors.toList());
        }
        return result;
    }

    public List<Recommendation> recommendStationToReturnBike(GeoPoint currentposition, GeoPoint destination) {
        List<Station> temp;
        List<Recommendation> result = new ArrayList<>();
        List<Station> stations = validStationsToReturnBike(stationManager.consultStations()).stream().
                filter(station -> station.getPosition().distanceTo(destination) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            Comparator<Station> bySlotsRatio = bySlotsCapacityRatio(destination);
            temp = stations.stream().sorted(bySlotsRatio).collect(Collectors.toList());
            result = temp.stream().map(s -> new Recommendation(s, null)).collect(Collectors.toList());
        } 

        return result;
    }
    public static Comparator<Station> byBikesCapacityRatio(GeoPoint pos) {
        return (s1, s2) -> {
            int i = Double.compare((double)s1.availableBikes()/(double)s1
				.getCapacity(), (double)s2.availableBikes()/(double)s2.getCapacity());
            if (i < 0) {
                return +1;
            }
            if (i > 0) {
                return -1;
            }
            return Double.compare(s1.getPosition().distanceTo(pos), s2.getPosition().distanceTo(pos));
        };
    }

    public static Comparator<Station> bySlotsCapacityRatio(GeoPoint pos) {
        return (s1, s2) -> {
            int i = Double.compare((double)s1.availableSlots()/(double)s1
				.getCapacity(), (double)s2.availableSlots()/(double)s2.getCapacity());
            if (i < 0) {
                return +1;
            }
            if (i > 0) {
                return -1;
            }
            return Double.compare(s1.getPosition().distanceTo(pos), s2.getPosition().distanceTo(pos));
        };
    }

}
