package es.urjc.ia.bikesurbanfleets.services.RecommendationSystems;

import es.urjc.ia.bikesurbanfleets.services.RecommendationSystems.Incentives.Incentive;
import es.urjc.ia.bikesurbanfleets.worldentities.stations.entities.Station;

public class Recommendation {

    private Station station;   // recommended station
    private Incentive incentive;   // discount
    private double probability;

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public Recommendation(Station station, Incentive incentive) {
        super();
        this.station = station;
        this.incentive = incentive;
    }

    public Station getStation() {
        return station;
    }

    public Incentive getIncentive() {
        return incentive;
    }

}
