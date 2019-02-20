package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystemTypes;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystems.incentives.Incentive;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.recommendationSystems.incentives.Money;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.InfrastructureManager;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;

@RecommendationSystemType("SURROUNDING_STATIONS_SIMPLE_INCENTIVES")
public class RecommendationSystemBySurroundingStationsWithSimpleIncentives extends RecommendationSystem {

    @RecommendationSystemParameters
    public class RecommendationParameters {

        private int maxDistanceRecommendation = 600;
        private double COMPENSATION = 10.0;
        private double EXTRA = 1.5;

    }

    private RecommendationParameters parameters;

    public RecommendationSystemBySurroundingStationsWithSimpleIncentives(JsonObject recomenderdef, SimulationServices ss) throws Exception {
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
        List<Station> stations = validStationsToRentBike(infrastructureManager.consultStations()).stream()
                .filter(station -> station.getPosition().distanceTo(point) <= parameters.maxDistanceRecommendation)
                .collect(Collectors.toList());
        List<StationUtilityData> qualities = new ArrayList<>();
        List<Station> allStations = infrastructureManager.consultStations();

        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            double quality = qualityToRent(allStations, station);
            StationUtilityData sd = new StationUtilityData(station);
            sd.setUtility(quality);
            qualities.add(sd);
        }

        Station nearestStation = nearestStationToRent(stations, point);

        Comparator<StationUtilityData> byQuality = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
        qualities = qualities.stream().sorted(byQuality).collect(Collectors.toList());

        List<Recommendation> recommendations = new ArrayList<>();
        int numStations = qualities.size();
        Incentive incentive = new Money(0);
        double compensation, extra;
        for (int i = 0; i < numStations; i++) {
            Station s = qualities.get(i).getStation();
            if (s.getId() != nearestStation.getId()) {
                compensation = compensation(point, nearestStation, s);
                extra = (numStations - i) * parameters.EXTRA;
                incentive = new Money((int) Math.round(compensation + extra));
            }
            recommendations.add(new Recommendation(s, incentive));
        }
        return recommendations;
    }

    @Override
    public List<Recommendation> recommendStationToReturnBike(GeoPoint currentposition, GeoPoint destination) {
        List<Station> stations = validStationsToReturnBike(infrastructureManager.consultStations()).stream()
                .filter(station -> station.getPosition().distanceTo(destination) <= parameters.maxDistanceRecommendation)
                .collect(Collectors.toList());
        List<StationUtilityData> qualities = new ArrayList<>();

        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            double quality = qualityToReturn(stations, station);
            StationUtilityData sd=new StationUtilityData(station);
            sd.setUtility(quality);
            qualities.add(sd);
        }

        Station nearestStation = nearestStationToReturn(stations, destination);

        Comparator<StationUtilityData> byQuality = (sq1, sq2) -> Double.compare(sq2.getUtility(), sq1.getUtility());
        qualities = qualities.stream().sorted(byQuality).collect(Collectors.toList());

        List<Recommendation> recommendations = new ArrayList<>();
        int numStations = qualities.size();
        Incentive incentive = new Money(0);
        double compensation, extra;
        for (int i = 0; i < numStations; i++) {
            Station s = qualities.get(i).getStation();
            if (s.getId() != nearestStation.getId()) {
                compensation = compensation(destination, nearestStation, s);
                extra = (numStations - i) * parameters.EXTRA;
                incentive = new Money((int) Math.round(compensation + extra));
            }
            recommendations.add(new Recommendation(s, incentive));
        }
        return recommendations;
    }

    private double qualityToRent(List<Station> stations, Station station) {
        double summation = 0;
        if (!stations.isEmpty()) {
            double factor = 0.0;
            double multiplication = 1.0;
            double maxDistance = parameters.maxDistanceRecommendation;
            double distance = 0.0;
            for (Station s : stations) {
                distance = station.getPosition().distanceTo(s.getPosition());
                if (maxDistance > distance) {
                    factor = (maxDistance - distance) / maxDistance;
                }
                multiplication = s.availableBikes() * factor;
                summation += multiplication;
            }
        }
        return summation;
    }

    private double qualityToReturn(List<Station> stations, Station station) {
        double summation = 0;
        if (!stations.isEmpty()) {
            double factor = 0.0;
            double multiplication = 1.0;
            double maxDistance = parameters.maxDistanceRecommendation;
            double distance = 0.0;

            for (Station s : stations) {
                distance = station.getPosition().distanceTo(s.getPosition());
                if (maxDistance > distance) {
                    factor = (maxDistance - distance) / maxDistance;
                }
                multiplication = s.availableSlots() * factor;
                summation += multiplication;
            }
        }
        return summation;
    }

    private Station nearestStationToRent(List<Station> stations, GeoPoint point) {
        Comparator<Station> byDistance = StationComparator.byDistance(point);
        List<Station> orderedStations = stations.stream().filter(s -> s.availableBikes() > 0)
                .sorted(byDistance).collect(Collectors.toList());
        return orderedStations.get(0);
    }

    private Station nearestStationToReturn(List<Station> stations, GeoPoint point) {
        Comparator<Station> byDistance = StationComparator.byDistance(point);
        List<Station> orderedStations = stations.stream().filter(s -> s.availableSlots() > 0)
                .sorted(byDistance).collect(Collectors.toList());
        return orderedStations.get(0);
    }

    private double compensation(GeoPoint point, Station nearestStation, Station recommendedStation) {
        double distanceToNearestStation = nearestStation.getPosition().distanceTo(point);
        double distanceToRecommendedStation = recommendedStation.getPosition().distanceTo(point);
        return (distanceToRecommendedStation - distanceToNearestStation) / parameters.COMPENSATION;
    }

}
