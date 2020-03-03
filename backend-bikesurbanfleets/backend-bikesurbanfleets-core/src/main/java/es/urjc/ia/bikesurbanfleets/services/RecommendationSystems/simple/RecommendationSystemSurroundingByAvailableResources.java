/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.simple;

import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.Recommendation;
import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.core.SimulationDateTime;
import es.urjc.ia.bikesurbanfleets.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.StationUtilityData;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.StationManager;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author holger
 */
@RecommendationSystemType("SURROUNDING_AVAILABLE_RESOURCES")
public class RecommendationSystemSurroundingByAvailableResources extends RecommendationSystem {

    public static class RecommendationParameters extends RecommendationSystem.RecommendationParameters{
        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        private int maxDistanceRecommendationReturn = 600;

        /**
         * It is the maximum distance in meters between a station and the
         * stations we take into account for checking the area
         */
        private double MaxDistanceSurroundingStations = 300;
    }

    private class StationSurroundingData {
        StationSurroundingData(Station s, double q, double d, Station n, double nd){
            station=s;
            quality=q;
            distance=d;
            nearest=n;
            nearestDistance=nd;
            
        }

        Station station = null;
        double quality = 0.0D;
        double distance = 0.0D;
        Station nearest=null;
        double nearestDistance=0.0D;
        
    }

    private RecommendationParameters parameters;

    public RecommendationSystemSurroundingByAvailableResources(JsonObject recomenderdef, SimulationServices ss) throws Exception {
        //***********Parameter treatment*****************************
        //parameters are read in the superclass
        //afterwards, they have to be cast to this parameters class
        super(recomenderdef, ss, new RecommendationParameters());
        this.parameters= (RecommendationParameters)(super.parameters);
    }


    @Override
    public List<Recommendation> recommendStationToRentBike(GeoPoint point, double maxdist) {
        List<Recommendation> result = new ArrayList<>();
        List<Station> candidatestations = stationsWithBikesInWalkingDistance( point,  maxdist);

        if (!candidatestations.isEmpty()) {
            List<StationSurroundingData> stationdata = getStationQualityRenting(candidatestations,point);
            List<StationSurroundingData> temp = stationdata.stream().sorted(byQuality(point)).collect(Collectors.toList());
            if (printHints) {
                printRecomendations(temp, true);
            }
            result = temp.stream().map(StationSurroundingData -> new Recommendation(StationSurroundingData.station, null)).collect(Collectors.toList());
        }
        return result;
    }

    public List<Recommendation> recommendStationToReturnBike(GeoPoint currentposition, GeoPoint destination) {
        List<Recommendation> result = new ArrayList<>();
        List<Station> candidatestations = stationsWithSlotsInWalkingDistance(destination,parameters.maxDistanceRecommendationReturn);

        if (!candidatestations.isEmpty()) {
            List<StationSurroundingData> stationdata = getStationQualityReturning(candidatestations, destination);
            List<StationSurroundingData> temp = stationdata.stream().sorted(byQuality(destination)).collect(Collectors.toList());
            if (printHints) {
                printRecomendations(temp, false);
            }
            result = temp.stream().map(StationSurroundingData -> new Recommendation(StationSurroundingData.station, null)).collect(Collectors.toList());
        } 

        return result;
    }
    
    private void printRecomendations(List<StationSurroundingData> su, boolean take) {
        if (printHints) {
        int max = su.size();//Math.min(5, su.size());
        System.out.println();
        if (take) {
            System.out.println("Time (take):" + SimulationDateTime.getCurrentSimulationDateTime());
        } else {
            System.out.println("Time (return):" + SimulationDateTime.getCurrentSimulationDateTime());
        }
        for (int i = 0; i < max; i++) {
            StationSurroundingData s = su.get(i);
            if (s.nearest!=null)
            System.out.format("Station %3d %2d %2d %10.2f %10.2f %3d %10.2f %2d%n", +
                    s.station.getId(),
                    s.station.availableBikes(),
                    s.station.getCapacity(),
                    s.distance,
                    s.quality,
                    s.nearest.getId(),
                    s.nearestDistance,
                    s.nearest.availableBikes());
            else 
            System.out.format("Station %3d %2d %2d %10.2f %10.2f %3d %n", +
                    s.station.getId(),
                    s.station.availableBikes(),
                    s.station.getCapacity(),
                    s.distance,
                    s.quality,
                    0);
            }
        }
    }
    private List<StationSurroundingData> getStationQualityRenting(List<Station> stations, GeoPoint pos) {
        List<StationSurroundingData> stationdat = new ArrayList<StationSurroundingData>();

        for (Station candidatestation : stations) {
            double summation = 0;
            List<Station> otherstations = stationManager.consultStations().stream()
                    .filter(other -> candidatestation.getPosition().eucleadeanDistanceTo(other.getPosition()) <= parameters.MaxDistanceSurroundingStations).collect(Collectors.toList());
            double factor, multiplication;
            double nearestdist=Double.MAX_VALUE;
            Station nearest=null;
            for (Station other : otherstations) {
                double dist=candidatestation.getPosition().eucleadeanDistanceTo(other.getPosition());
                factor = (parameters.MaxDistanceSurroundingStations - dist) / parameters.MaxDistanceSurroundingStations;
                multiplication = other.availableBikes() * factor;
                summation += multiplication;
                if (dist<nearestdist && other.getId()!=candidatestation.getId()){
                    nearest=other;
                    nearestdist=dist;
                }
            }
            double dist=graphManager.estimateDistance(pos, candidatestation.getPosition() ,"foot");
            stationdat.add(new StationSurroundingData(candidatestation,summation, dist, nearest,nearestdist));
        }
        return stationdat;
    }
    
    private List<StationSurroundingData> getStationQualityReturning(List<Station> stations, GeoPoint dest) {
        List<StationSurroundingData> stationdat = new ArrayList<StationSurroundingData>();

        for (Station candidatestation : stations) {
            double nearestdist=Double.MAX_VALUE;
            Station nearest=null;
           double summation = 0;
            List<Station> otherstations = stationManager.consultStations().stream()
                    .filter(other -> candidatestation.getPosition().eucleadeanDistanceTo(other.getPosition()) <= parameters.MaxDistanceSurroundingStations).collect(Collectors.toList());
            double factor, multiplication;
            for (Station other : otherstations) {
                double dist=candidatestation.getPosition().eucleadeanDistanceTo(other.getPosition());
                factor = (parameters.MaxDistanceSurroundingStations - dist) / parameters.MaxDistanceSurroundingStations;
                multiplication = other.availableSlots() * factor;
                summation += multiplication;
                if (dist<nearestdist && other.getId()!=candidatestation.getId()){
                    nearest=other;
                    nearestdist=dist;
                }
            }
            double dist=graphManager.estimateDistance(candidatestation.getPosition(), dest ,"foot");
            stationdat.add(new StationSurroundingData(candidatestation,summation,dist, nearest,nearestdist));
        }
        return stationdat;
    }
 
    public  Comparator<StationSurroundingData> byQuality(GeoPoint pos) {
        return (s1, s2) -> {
            int i = Double.compare(s1.quality, s2.quality);
            if (i < 0) {
                return +1;
            }
            if (i > 0) {
                return -1;
            }
            return Double.compare(
                    graphManager.estimateDistance(s1.station.getPosition(), pos ,"foot"),
                    graphManager.estimateDistance(s2.station.getPosition(), pos ,"foot")
            );
        };
    }

 }
