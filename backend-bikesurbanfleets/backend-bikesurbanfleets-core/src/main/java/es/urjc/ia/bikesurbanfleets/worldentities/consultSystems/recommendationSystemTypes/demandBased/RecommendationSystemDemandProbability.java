package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes.demandBased;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.core.SimulationDateTime;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes.Recommendation;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes.StationUtilityData;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.InfrastructureManager;
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
@RecommendationSystemType("DEMAND_PROBABILITY")
public class RecommendationSystemDemandProbability extends RecommendationSystem {

    @RecommendationSystemParameters
    public class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendation = 600;
        private double wheightDistanceStationUtility = 0.3;
        //this is meters per second corresponds aprox. to 4 and 20 km/h
        private double walkingVelocity = 1.12;
        private double cyclingVelocity = 6.0;


    }

    private RecommendationParameters parameters;

    public RecommendationSystemDemandProbability(JsonObject recomenderdef, SimulationServices ss) throws Exception {
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
        //       demandManager=infraestructureManager.getDemandManager();
    }

    @Override
    public List<Recommendation> recommendStationToRentBike(GeoPoint currentposition) {
        List<Recommendation> result;
        List<Station> stations = validStationsToRentBike(infrastructureManager.consultStations()).stream()
                .filter(station -> station.getPosition().distanceTo(currentposition) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su = getStationUtility(stations, null, currentposition, true);
            Comparator<StationUtilityData> DescUtility = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
            List<StationUtilityData> temp = su.stream().sorted(DescUtility).collect(Collectors.toList());
   
            System.out.println();
            System.out.println("Time:" +SimulationDateTime.getCurrentSimulationDateTime());
            temp.forEach(s -> {
                System.out.println("Station (take)" + s.getStation().getId() + ": "
                    + s.getStation().availableBikes() + " "
                    + s.getStation().getCapacity() + " "
                    + s.getOptimalocupation() + " "
                    + s.getDistance() + " "
                    + s.getUtility());
               
            });
            result = temp.stream().map(sq -> new Recommendation(sq.getStation(), null)).collect(Collectors.toList());
        } else {
            result = new ArrayList<>();
        }
        return result;
    }

    public List<Recommendation> recommendStationToReturnBike(GeoPoint currentposition, GeoPoint destination) {
        List<Recommendation> result = new ArrayList<>();
        List<Station> stations = validStationsToReturnBike(infrastructureManager.consultStations()).stream().
                filter(station -> station.getPosition().distanceTo(destination) <= parameters.maxDistanceRecommendation).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su = getStationUtility(stations, destination, currentposition, false);
            Comparator<StationUtilityData> byDescUtilityIncrement = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
            List<StationUtilityData> temp = su.stream().sorted(byDescUtilityIncrement).collect(Collectors.toList());
            System.out.println();
            System.out.println("Time:" +SimulationDateTime.getCurrentSimulationDateTime());
            temp.forEach(s -> {
   
                System.out.println("Station (return)" + s.getStation().getId() + ": "
                    + s.getStation().availableBikes() + " "
                    + s.getStation().getCapacity() + " "
                     + s.getOptimalocupation() + " "
                    + s.getDistance() + " "
                    + s.getUtility());
                
            });
            result = temp.stream().map(sq -> new Recommendation(sq.getStation(), null)).collect(Collectors.toList());
        } 
        return result;
    }

    public List<StationUtilityData> getStationUtility(List<Station> stations, GeoPoint destination, GeoPoint currentposition, boolean rentbike) {
        InfrastructureManager.UsageData ud = infrastructureManager.getCurrentUsagedata();
        List<StationUtilityData> temp = new ArrayList<>();
        for (Station s : stations) {
 
     
            StationUtilityData sd = new StationUtilityData(s);

            double prob=0;
            double dist=0;
            if (rentbike){
                dist = currentposition.distanceTo(s.getPosition());
                prob=infrastructureManager.getAvailableBikeProbability(s, dist/this.parameters.walkingVelocity);
            } else {
                dist = currentposition.distanceTo(s.getPosition());
                prob=infrastructureManager.getAvailableSlotProbability(s, dist/this.parameters.cyclingVelocity);
                dist = s.getPosition().distanceTo(destination);
            }
                    
            double norm_distance = 1 - normatizeTo01(dist, 0, parameters.maxDistanceRecommendation);
 //           double globalutility = parameters.wheightDistanceStationUtility * norm_distance
 //                   + (1 - parameters.wheightDistanceStationUtility) * normedUtilityDiff;
            double globalutility =  norm_distance * prob;

            /*       double mincap=(double)infraestructureManager.getMinStationCapacity();
            double maxinc=(4D*(mincap-1))/Math.pow(mincap,2);
            double auxnormutil=((newutility-utility+maxinc)/(2*maxinc));
            double globalutility= dist/auxnormutil; 
             */
            sd.setUtility(globalutility);
            sd.setOptimalocupation(prob);
            sd.setDistance(dist);
            temp.add(sd);
        }
        return temp;
    }

    private double getUtility(Station s, int bikeincrement, double idealbikes, double maxidealbikes) {
        double cap = s.getCapacity();
        double ocupation = s.availableBikes() + bikeincrement;
        if (idealbikes <= maxidealbikes) {
            if (ocupation <= idealbikes) {
                return 1 - Math.pow(((ocupation - idealbikes) / idealbikes), 2);
            } else if (ocupation >= maxidealbikes) {
                return 1 - Math.pow(((ocupation - maxidealbikes) / (cap - maxidealbikes)), 2);
            } else {//if ocupation is just between max and min
                return 1;
            }
        } else { //idealbikes > max idealbikes
            double bestocupation = (idealbikes  + maxidealbikes) / 2D;
  //          double bestocupation = (idealbikes * cap)/(cap - maxidealbikes  ) ;
            if (ocupation <= bestocupation) {
                return 1 - Math.pow(((ocupation - bestocupation) / bestocupation), 2);
            } else {
                double aux = cap - bestocupation;
                return 1 - Math.pow(((ocupation - bestocupation) / aux), 2);
            }

        }
    }
}
