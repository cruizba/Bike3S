package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.ComplexRecommendationSystemTypes;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.StationUtilityData;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a system which recommends the user the stations to which he
 * should go to contribute with system rebalancing. Then, this recommendation
 * system gives the user a list of stations ordered descending by the
 * "resources/capacityº" ratio.
 *
 * @author IAgroup
 *
 */
@RecommendationSystemType("DEMAND_PROBABILITY_expected_compromised")
public class RecommendationSystemDemandProbability extends RecommendationSystemDemandProbabilityBased {

    @RecommendationSystemParameters
    public class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendation = 600;

        private double upperProbabilityBound = 0.999;
        private double desireableProbability = 0.6;

        private double factor = 1D / (double) (2000);
    }
    private RecommendationParameters parameters;

    public RecommendationSystemDemandProbability(JsonObject recomenderdef, SimulationServices ss) throws Exception {
        super(recomenderdef, ss);
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
    protected List<StationUtilityData> specificOrderStationsRent(List<StationUtilityData> stationdata, List<Station> allstations, GeoPoint currentuserposition) {
        List<StationUtilityData> orderedlist = new ArrayList<>();
        for (StationUtilityData sd : stationdata) {
            addrent(sd, orderedlist);
        }
        return orderedlist;
    }

    @Override
    protected List<StationUtilityData> specificOrderStationsReturn(List<StationUtilityData> stationdata, List<Station> allstations, GeoPoint currentuserposition, GeoPoint userdestination) {
        List<StationUtilityData> orderedlist = new ArrayList<>();
        for (StationUtilityData sd : stationdata) {
            addreturn(sd, orderedlist);
        }
        return orderedlist;
    }

    //take into account that distance newSD >= distance oldSD
    protected boolean betterOrSameRent(StationUtilityData newSD, StationUtilityData oldSD) {
        if (oldSD.getProbabilityTake() > this.parameters.upperProbabilityBound) {
            return false;
        }
        if (newSD.getProbabilityTake() <= oldSD.getProbabilityTake()) {
            return false;
        }
        // if here newSD.getProbability() > oldSD.getProbability()
        if (newSD.getWalkdist() <= this.parameters.maxDistanceRecommendation) {
            if (oldSD.getProbabilityTake() > this.parameters.desireableProbability) {
                double timediff = (newSD.getWalkdist() - oldSD.getWalkdist()) * this.parameters.factor;
                double probdiff = newSD.getProbabilityTake() - oldSD.getProbabilityTake();
                if (probdiff > timediff) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (oldSD.getWalkdist() <= this.parameters.maxDistanceRecommendation) {
            return false;
        }
        double timediff = (newSD.getWalkdist() - oldSD.getWalkdist()) * this.parameters.factor;
        double probdiff = newSD.getProbabilityTake() - oldSD.getProbabilityTake();
        if (probdiff > timediff) {
            return true;
        }
        return false;
    }

 
    //take into account that distance newSD >= distance oldSD
    protected boolean betterOrSameReturn(StationUtilityData newSD, StationUtilityData oldSD) {
        if (oldSD.getProbabilityReturn() > this.parameters.upperProbabilityBound) {
            return false;
        }
        if (newSD.getProbabilityReturn() <= oldSD.getProbabilityReturn()) {
            return false;
        }
        // if here  newSD.getProbability() > oldSD.getProbability()
        if (oldSD.getProbabilityReturn() > this.parameters.desireableProbability) {
            double timediff = (newSD.getWalkdist()- oldSD.getWalkdist()) * this.parameters.factor;
            double probdiff = newSD.getProbabilityReturn() - oldSD.getProbabilityReturn();
            if (probdiff > timediff) {
                return true;
            }
            return false;
        }
        if (newSD.getProbabilityReturn() >= this.parameters.desireableProbability) {
            return true;
        }
        double timediff = (newSD.getWalkdist() - oldSD.getWalkdist()) * this.parameters.factor;
        double probdiff = newSD.getProbabilityReturn() - oldSD.getProbabilityReturn();
        if (probdiff > timediff) {
            return true;
        }
        return false;
    }
 }
