package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;
import java.util.Objects;

public class StationUtilityData {

    private Station station;
    private double Utility;
   
    private double time;
    private double minoptimalocupation;
    private double maxopimalocupation;
    private double optimalocupation;
    private double capacity;
    private double ocupation;
    private double probability;
    private double probabilityTake;
    private double probabilityReturn;
    private double probabilityTakeAfter;
    private double probabilityReturnAfter;
    private double distance;
    public int closest;
    public double closestbikedist;
    public double closestprob;
    public double bikedist;
    public double walkdist;
    public double closestwalkdist;
    private double totalCost;
    private double Cost;
    private double takecostdiff;
    private double returncostdiff;

    public double getTotalCost() {
        return totalCost;
    }

    public StationUtilityData setTotalCost(double totalCost) {
        this.totalCost = totalCost;
        return this;           
    }

    public double getTakecostdiff() {
        return takecostdiff;
    }

    public StationUtilityData setTakecostdiff(double takecostdiff) {
        this.takecostdiff = takecostdiff;
        return this;           
    }

    public double getReturncostdiff() {
        return returncostdiff;
    }

    public StationUtilityData setReturncostdiff(double returncostdiff) {
        this.returncostdiff = returncostdiff;
        return this;           
    }

    public double getProbabilityTake() {
        return probabilityTake;
    }

    public StationUtilityData setProbabilityTake(double probabilityTake) {
        this.probabilityTake = probabilityTake;
        return this;           
    }

    public double getProbabilityReturn() {
        return probabilityReturn;
    }

    public StationUtilityData setProbabilityReturn(double probabilityReturn) {
        this.probabilityReturn = probabilityReturn;
        return this;           
    }

    public double getProbabilityTakeAfter() {
        return probabilityTakeAfter;
    }

    public StationUtilityData setProbabilityTakeAfter(double probabilityTakeAfter) {
        this.probabilityTakeAfter = probabilityTakeAfter;
        return this;           
    }

    public double getProbabilityReturnAfter() {
        return probabilityReturnAfter;
    }

    public StationUtilityData setProbabilityReturnAfter(double probabilityReturnAfter) {
        this.probabilityReturnAfter = probabilityReturnAfter;
        return this;           
    }

    public double getCost() {
        return Cost;
    }

    public StationUtilityData setCost(double Cost) {
        this.Cost = Cost;
        return this;           
    }

    public double getDistance() {
        return distance;
    }

    public StationUtilityData setDistance(double distance) {
        this.distance = distance;
        return this;           
    }

    public double getCapacity() {
        return capacity;
    }

    public StationUtilityData setCapacity(double capacity) {
        this.capacity = capacity;
        return this;
    }

    public double getOcupation() {
        return ocupation;
    }

    public StationUtilityData setOcupation(double ocupation) {
        this.ocupation = ocupation;
        return this;
    }

    public double getTime() {
        return time;
    }

    public StationUtilityData setTime(double time) {
        this.time = time;
        return this;
    }

    public StationUtilityData(Station station) {
        super();
        this.station = station;
    }

    public StationUtilityData setUtility(double Utility) {
        this.Utility = Utility;
        return this;
    }

    public double getUtility() {
        return Utility;
    }

    public Station getStation() {
        return station;
    }

    public double getMinoptimalocupation() {
        return minoptimalocupation;
    }

    public StationUtilityData setMinoptimalocupation(double minoptimalocupation) {
        this.minoptimalocupation = minoptimalocupation;
        return this;
    }

    public double getMaxopimalocupation() {
        return maxopimalocupation;
    }

    public StationUtilityData setMaxopimalocupation(double maxopimalocupation) {
        this.maxopimalocupation = maxopimalocupation;
        return this;
    }

    public double getOptimalocupation() {
        return optimalocupation;
    }

    public StationUtilityData setOptimalocupation(double optimalocupation) {
        this.optimalocupation = optimalocupation;
        return this;
    }

    public double getProbability() {
        return probability;
    }

    public StationUtilityData setProbability(double probability) {
        this.probability = probability;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return false;
    }

}
