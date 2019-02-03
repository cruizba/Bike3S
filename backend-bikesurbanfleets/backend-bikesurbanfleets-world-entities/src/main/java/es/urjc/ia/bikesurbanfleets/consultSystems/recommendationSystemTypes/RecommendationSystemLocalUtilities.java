package es.urjc.ia.bikesurbanfleets.consultSystems.recommendationSystemTypes;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.infraestructure.InfraestructureManager;
import es.urjc.ia.bikesurbanfleets.infraestructure.entities.Station;

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
@RecommendationSystemType("LOCAL_RESOURCES_PROPORTION")
public class RecommendationSystemLocalUtilities extends RecommendationSystem {

    @RecommendationSystemParameters
    public class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendation = 600;
        private double wheightDistanceStationUtility=0.5;
 
    }

    private RecommendationParameters parameters;

    public RecommendationSystemLocalUtilities(JsonObject recomenderdef, InfraestructureManager infraestructureManager) throws Exception {
        super(infraestructureManager);
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
            Comparator<StationUtilityData> byDescUtilityIncrement = (sq1, sq2) -> Double.compare(sq2.getUtilityIncrement(), sq1.getUtilityIncrement());
            List<StationUtilityData> temp=su.stream().sorted(byDescUtilityIncrement).collect(Collectors.toList());
            temp.forEach(s -> System.out.println("Station (take)" + s.getStation().getId() + ": " + (double) s.getStation().availableBikes() / s.getStation().getCapacity()));
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
            temp.forEach(s -> System.out.println("Station (return)" + s.getStation().getId() + ": " + (double) s.getStation().availableBikes() / s.getStation().getCapacity()));
            result= temp.stream().map(sq -> new Recommendation(sq.getStation(), null)).collect(Collectors.toList());
        } else {
            result=new ArrayList<>();
        }
        return result;
    }
    
    public List<StationUtilityData> getStationUtility(List<Station> stations,GeoPoint point, boolean rentbike) {
        List<StationUtilityData> temp=new ArrayList<>();
        for (Station s:stations){
            double idealAvailable=s.getCapacity()/2D;
//            double utility=1- Math.abs(s.availableBikes()- idealAvailable)/idealAvailable;
            double utility=1-Math.pow((s.availableBikes()/idealAvailable)-1,2);
            double newutility;
            if (rentbike){
                newutility=1-Math.pow(((s.availableBikes()-1)/idealAvailable)-1,2);
            } else {//return bike 
                newutility=1-Math.pow(((s.availableBikes()+1)/idealAvailable)-1,2);
            }
            double minutilityimprovement=4*(1-infraestructureManager.getMinStationCapacity())/
                    (infraestructureManager.getMinStationCapacity()*infraestructureManager.getMinStationCapacity());
            double maxutilityimprovement=4*(infraestructureManager.getMinStationCapacity()-1)/
                    (infraestructureManager.getMinStationCapacity()*infraestructureManager.getMinStationCapacity());
            double norm_utility=normatizeToUtility(newutility-utility,minutilityimprovement,maxutilityimprovement);
            double dist=point.distanceTo(s.getPosition());
            double norm_distance=normatizeToUtility(newutility-utility,0,parameters.maxDistanceRecommendation);
            double globalutility=parameters.wheightDistanceStationUtility*norm_distance+
                    (1-parameters.wheightDistanceStationUtility)*norm_utility;
            temp.add(new StationUtilityData(s,globalutility));
        }
        return temp;
    }

}
