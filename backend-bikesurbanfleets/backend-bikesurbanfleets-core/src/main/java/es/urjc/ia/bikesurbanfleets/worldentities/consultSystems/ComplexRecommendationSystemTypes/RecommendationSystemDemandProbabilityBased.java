package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.ComplexRecommendationSystemTypes;

import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import es.urjc.ia.bikesurbanfleets.core.core.SimulationDateTime;
import es.urjc.ia.bikesurbanfleets.core.services.SimulationServices;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.Recommendation;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.StationUtilityData;
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
public abstract class RecommendationSystemDemandProbabilityBased extends RecommendationSystem {

    private class RecommendationParameters {

        /**
         * It is the maximum distance in meters between the recommended stations
         * and the indicated geographical point.
         */
        // the velocities here are real (estimated velocities)
        // assuming real velocities of 1.4 m/s and 6 m/s for walking and biking (aprox. to 5 and 20 km/h)
        //Later the velocities are adjusted to straight line velocities
        //given a straight line distance d, the real distance dr may be estimated  
        // as dr=f*d, whewre f will be between 1 and sqrt(2) (if triangle).
        // here we consider f=1.4
        //to translate velocities from realdistances to straight line distances:
        // Vel_straightline=(d/dr)*vel_real -> Vel_straightline=vel_real/f
        //assuming real velocities of 1.4 m/s and 6 m/s for walking and biking (aprox. to 5 and 20 km/h)
        //the adapted straight line velocities are: 1m/s and 4.286m/s
        public double walkingVelocity = 1.4D;
        public double cyclingVelocity = 6D;
        public double probabilityUsersObey = 1D;
        public boolean takeintoaccountexpected = true;
        public boolean takeintoaccountcompromised = true;
    }

    protected double straightLineWalkingVelocity ;
    protected double straightLineCyclingVelocity ;

    boolean printHints = true;
    protected RecommendationParameters baseparameters;
    protected UtilitiesForRecommendationSystems recutils;
    private PastRecommendations pastrecs;
    
    public RecommendationSystemDemandProbabilityBased(JsonObject recomenderdef, SimulationServices ss) throws Exception {
        super(ss);
        //***********Parameter treatment*****************************
        //if this recomender has parameters this is the right declaration
        //if no parameters are used this code just has to be commented
        //"getparameters" is defined in USER such that a value of Parameters 
        // is overwritten if there is a values specified in the jason description of the recomender
        // if no value is specified in jason, then the orriginal value of that field is mantained
        // that means that teh paramerts are all optional
        // if you want another behaviour, then you should overwrite getParameters in this calss
        this.baseparameters = new RecommendationParameters();
        getParameters(recomenderdef, this.baseparameters);
        //calculate the corresponding straightline velocities
        // the distances here are straight line distances
        //given a straight line distance d, the real distance dr may be estimated  
        // as dr=f*d, whewre f will be between 1 and sqrt(2) (if triangle).
        // here we consider f=1.4
        //to translate velocities from realdistances to straight line distances:
        // Vel_straightline=(d/dr)*vel_real -> Vel_straightline=vel_real/f
        //assuming real velocities of 1.4 m/s and 6 m/s for walking and biking (aprox. to 5 and 20 km/h)
        //the adapted straight line velocities are: 1m/s and 4.286m/s
        straightLineWalkingVelocity = this.baseparameters.walkingVelocity/1.4;
        straightLineCyclingVelocity = this.baseparameters.cyclingVelocity/1.4;
        
        recutils=new UtilitiesForRecommendationSystems(this);
        pastrecs=new PastRecommendations();
    }
    
    private static Comparator<Station> byDistance(GeoPoint point) {
        return (s1, s2) -> Double.compare(s1.getPosition().distanceTo(point), s2.getPosition().distanceTo(point));
    }


    @Override
    public List<Recommendation> recommendStationToRentBike(GeoPoint currentposition) {
        List<Recommendation> result;
        List<Station> stations = infrastructureManager.consultStations().stream().
                sorted(byDistance(currentposition)).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su = getOrderedStationsRent(stations, currentposition);
            if (su.size() == 0) {
                System.out.println("ERROR take: no recommendation found with minimal parameters at Time:" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
            }
            if (printHints) {
                printRecomendations(su, true);
            }
            result = su.stream().map(sq -> {
                Recommendation r = new Recommendation(sq.getStation(), null);
                r.setProbability(sq.getProbabilityTake());
                return r;
            }
            ).collect(Collectors.toList());
            //add values to the expeted takes
            StationUtilityData first = su.get(0);
            double dist = currentposition.distanceTo(first.getStation().getPosition());
            pastrecs.addExpectedBikechange(first.getStation().getId(),
                    (int) (dist / straightLineWalkingVelocity), true);
        } else {
            result = new ArrayList<>();
            System.out.println("no recommendation for take at Time:" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
        }
        return result;
    }
    
    public List<Recommendation> recommendStationToReturnBike(GeoPoint currentposition, GeoPoint destination) {
        List<Recommendation> result = new ArrayList<>();
        List<Station> stations = infrastructureManager.consultStations().stream().
                sorted(byDistance(destination)).collect(Collectors.toList());

        if (!stations.isEmpty()) {
            List<StationUtilityData> su = getOrderedStationsReturn(stations, destination, currentposition);
            if (su.size() == 0) {
                System.out.println("ERROR return: no recommendation found with minimal parameters at Time:" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
            }
            if (printHints) {
                printRecomendations(su, false);
            }
            result = su.stream().map(sq -> {
                Recommendation r = new Recommendation(sq.getStation(), null);
                r.setProbability(sq.getProbabilityReturn());
                return r;
            }
            ).collect(Collectors.toList());
            //add values to the expeted returns
            StationUtilityData first = su.get(0);
            double dist = currentposition.distanceTo(first.getStation().getPosition());
            pastrecs.addExpectedBikechange(first.getStation().getId(),
                    (int) (dist / straightLineCyclingVelocity), false);
        } else {
            System.out.println("no recommendation for return at Time:" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
        }
        return result;
    }
    private int lowprobs = 0;
    private double probsr = 0D;
    private int callsr = 0;
    private double probst = 0D;
    private int callst = 0;

    private void printRecomendations(List<StationUtilityData> su, boolean take) {
        if (printHints) {
            int max = Math.min(10, su.size());
            System.out.println();
            if (take) {
                System.out.println("Time (take):" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
                probst += su.get(0).getProbabilityTake();
                callst++;
                System.out.format("Expected successrate take: %9.8f %n", (probst / callst));

                if (su.get(0).getProbabilityTake() < 0.6) {
                    System.out.format("LOW PROB Take %9.8f %n", su.get(0).getProbabilityTake());
                    lowprobs++;
                }
                System.out.println("         id av ca   wdist   wtime    prob   indcost tcostdiff  rostdiff   totcost bestn disttobn timetobn bnwd bnwt bnprob");
                for (int i = 0; i < max; i++) {
                    StationUtilityData s = su.get(i);
                    System.out.format("Station %3d %2d %2d %7.1f %7.1f %6.5f %9.2f %9.2f %9.2f %9.2f",
                            s.getStation().getId(),
                            s.getStation().availableBikes(),
                            s.getStation().getCapacity(),
                            s.getWalkdist(),
                            s.getWalkTime(),
                            s.getProbabilityTake(),
                            s.getIndividualCost(),
                            s.getTakecostdiff(),
                            s.getReturncostdiff(),
                            s.getTotalCost());
                    StationUtilityData bn=s.bestNeighbour;
                    if (bn!=null){
                        double distto=bn.getStation().getPosition().distanceTo(s.getStation().getPosition());
                        double timeto= (distto / straightLineWalkingVelocity);
                        System.out.format(" %3d %7.1f %7.1f %7.1f %7.1f %6.5f %n",
                            bn.getStation().getId(),
                            distto,
                            timeto,
                            bn.getWalkdist(),
                            bn.getWalkTime(),
                            bn.getProbabilityTake());     
                    } else {
                        System.out.println("");
                    }
                }
            } else {
                System.out.println("Time (return):" + SimulationDateTime.getCurrentSimulationDateTime()+ "("+SimulationDateTime.getCurrentSimulationInstant()+")");
                probsr += su.get(0).getProbabilityReturn();
                callsr++;
                System.out.format("Expected successrate return: %9.8f %n", (probsr / callsr));

                if (su.get(0).getProbabilityReturn() < 0.6) {
                    System.out.format("LOW PROB Return %9.8f %n", su.get(0).getProbabilityReturn());
                    lowprobs++;
                }
                System.out.println("         id av ca   wdist   wtime   bdist   btime    prob   indcost tcostdiff  rostdiff   totcost bestn disttobn timetobn bnwd bnwt bnprob");
                for (int i = 0; i < max; i++) {
                    StationUtilityData s = su.get(i);
                    System.out.format("Station %3d %2d %2d %7.1f %7.1f %7.1f %7.1f %6.5f %9.2f %9.2f %9.2f %9.2f",
                            s.getStation().getId(),
                            s.getStation().availableBikes(),
                            s.getStation().getCapacity(),
                            s.getWalkdist(),
                            s.getWalkTime(),
                            s.getBikedist(),
                            s.getBiketime(),
                            s.getProbabilityReturn(),
                            s.getIndividualCost(),
                            s.getTakecostdiff(),
                            s.getReturncostdiff(),
                            s.getTotalCost());
                    StationUtilityData bn=s.bestNeighbour;
                    if (bn!=null){
                        double distto=bn.getStation().getPosition().distanceTo(s.getStation().getPosition());
                        double timeto= (distto / straightLineCyclingVelocity);
                        System.out.format(" %3d %7.1f %7.1f %7.1f %7.1f %6.5f %n",
                            bn.getStation().getId(),
                            distto,
                            timeto,
                            bn.getWalkdist(),
                            bn.getWalkTime(),
                            bn.getProbabilityReturn());     
                    } else {
                        System.out.println("");
                    }
                }
            }
        }
    }
    
    //the list of stations is ordered by distance to currentposition
    private List<StationUtilityData> getOrderedStationsRent(List<Station> stations, GeoPoint currentposition) {
        List<StationUtilityData> temp = new ArrayList<>();
        for (Station s : stations) {
            StationUtilityData sd = new StationUtilityData(s);
            double dist = currentposition.distanceTo(s.getPosition());
            int offtime = (int) (dist / straightLineWalkingVelocity);
            sd.setWalkTime(offtime).setWalkdist(dist).setCapacity(s.getCapacity());
            recutils.calculateProbabilities(sd, offtime, baseparameters.takeintoaccountexpected, 
                    baseparameters.takeintoaccountcompromised,pastrecs, baseparameters.probabilityUsersObey);
            temp.add(sd);
        }
        List<StationUtilityData> ret=specificOrderStationsRent(temp,stations, currentposition);
        return ret;    
    }

    //the list of stations is ordered by distance to destination
    private List<StationUtilityData> getOrderedStationsReturn(List<Station> stations, GeoPoint destination, GeoPoint currentposition) {
        List<StationUtilityData> temp = new ArrayList<>();
        for (Station s : stations) {
            StationUtilityData sd = new StationUtilityData(s);
            double bikedist=currentposition.distanceTo(s.getPosition());
            int biketime = (int) (bikedist / straightLineCyclingVelocity);
            double walkdist=s.getPosition().distanceTo(destination);
            int walktime = (int) (walkdist / straightLineWalkingVelocity);
            sd.setWalkTime(walktime).setWalkdist(walkdist)
                    .setCapacity(s.getCapacity())
                    .setBikedist(bikedist).setBiketime(biketime);
            recutils.calculateProbabilities(sd, biketime, baseparameters.takeintoaccountexpected, 
                    baseparameters.takeintoaccountcompromised,pastrecs, baseparameters.probabilityUsersObey);
            temp.add(sd);
        }
        List<StationUtilityData> ret=specificOrderStationsReturn(temp,stations, currentposition, destination);
        return ret;    
    }

    abstract protected  List<StationUtilityData> specificOrderStationsRent(List<StationUtilityData> stationdata, List<Station> allstations, GeoPoint currentuserposition );
    abstract protected  List<StationUtilityData> specificOrderStationsReturn(List<StationUtilityData> stationdata, List<Station> allstations, GeoPoint currentuserposition, GeoPoint userdestination );   
    
    //methods for ordering the StationUtilityData should be true if the first data should be recomended befor the second
    //take into account that distance newSD >= distance oldSD
    abstract protected boolean betterOrSameRent(StationUtilityData newSD, StationUtilityData oldSD);
    abstract protected boolean betterOrSameReturn(StationUtilityData newSD, StationUtilityData oldSD);

    protected void addrent(StationUtilityData d, List<StationUtilityData> temp) {
        int i = 0;
        for (; i < temp.size(); i++) {
            if (betterOrSameRent(d, temp.get(i))) {
                break;
            }
        }
        temp.add(i, d);
    }

    protected void addreturn(StationUtilityData d, List<StationUtilityData> temp) {
        int i = 0;
        for (; i < temp.size(); i++) {
            if (betterOrSameReturn(d, temp.get(i))) {
                break;
            }
        }
        temp.add(i, d);
    }
}
