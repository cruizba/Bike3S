package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.InfraestructureManager;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;

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
@RecommendationSystemType("LOCAL_UTILITY_W_DISTANCE")
public class RecommendationSystemLocalUtilitiesWithDistance extends RecommendationSystem {

    @RecommendationSystemParameters
    public class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendation = 600;
        private double wheightDistanceStationUtility=0.3;
 
    }

    private RecommendationParameters parameters;

    public RecommendationSystemLocalUtilitiesWithDistance(JsonObject recomenderdef, SimulationServices ss) throws Exception {
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
        List<Recommendation> result;
        List<Station> stations = validStationsToRentBike(infraestructureManager.consultStations()).stream()
                .filter(station -> station.getPosition().distanceTo(point) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su=getStationUtility(stations,point, true);
            Comparator<StationUtilityData> DescUtility = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
            List<StationUtilityData> temp=su.stream().sorted(DescUtility).collect(Collectors.toList());
            System.out.println();
            temp.forEach(s -> System.out.println("Station (take)" + s.getStation().getId() + ": "
                    + s.getStation().availableBikes() + " "
                    + s.getStation().getCapacity() + " " 
                    + s.getOptimalocupation() + " "
                    + s.getDistance() + " "
                    + s.getUtility() ));
            result= temp.stream().map(sq -> new Recommendation(sq.getStation(), null)).collect(Collectors.toList());
        } else {
            result=new ArrayList<>();
        }
        return result;
    }

    public List<Recommendation> recommendStationToReturnBike(GeoPoint point) {
        List<Recommendation> result;
        List<Station> stations = validStationsToReturnBike(infraestructureManager.consultStations()).stream().
                filter(station -> station.getPosition().distanceTo(point) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su=getStationUtility(stations,point, false);
            Comparator<StationUtilityData> byDescUtilityIncrement = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
            List<StationUtilityData> temp=su.stream().sorted(byDescUtilityIncrement).collect(Collectors.toList());
            System.out.println();
            temp.forEach(s -> System.out.println("Station (return)" + s.getStation().getId() + ": "
                    + s.getStation().availableBikes() + " "
                    + s.getStation().getCapacity() + " " 
                    + s.getOptimalocupation() + " "
                    + s.getDistance() + " "
                    + s.getUtility() ));
            result= temp.stream().map(sq -> new Recommendation(sq.getStation(), null)).collect(Collectors.toList());
        } else { //if no best station has been found in the max distance
           Comparator<Station> byDistance = StationComparator.byDistance(point);
           List<Station> temp = validStationsToReturnBike(infraestructureManager.consultStations()).stream().sorted(byDistance).collect(Collectors.toList());
           result = temp.stream().map(s -> new Recommendation(s, null)).collect(Collectors.toList());           
        }
        return result;
    }
    
    public List<StationUtilityData> getStationUtility(List<Station> stations,GeoPoint point, boolean rentbike) {
        List<StationUtilityData> temp=new ArrayList<>();
        for (Station s:stations){
            double idealAvailable=s.getCapacity()/2D;
            double utility=1-Math.pow((s.availableBikes()/idealAvailable)-1,2);
            double newutility;
            if (rentbike){
                newutility=1-Math.pow(((s.availableBikes()-1)/idealAvailable)-1,2);
            } else {//return bike 
                newutility=1-Math.pow(((s.availableBikes()+1)/idealAvailable)-1,2);
            }
            double dist=point.distanceTo(s.getPosition());
            double norm_distance=1-normatizeTo01(dist,0,parameters.maxDistanceRecommendation);
            double globalutility=parameters.wheightDistanceStationUtility*norm_distance+
                    (1-parameters.wheightDistanceStationUtility)*(newutility-utility);
     /*       double mincap=(double)infraestructureManager.getMinStationCapacity();
            double maxinc=(4D*(mincap-1))/Math.pow(mincap,2);
            double auxnormutil=((newutility-utility+maxinc)/(2*maxinc));
            double globalutility= dist/auxnormutil; 
      */      
            StationUtilityData sd=new StationUtilityData(s);
            sd.setUtility(globalutility);
            sd.setDistance(dist);
            sd.setOptimalocupation(idealAvailable);
            temp.add(sd);
        }
        return temp;
    }

}
