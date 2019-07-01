/*
Description: The idea of cost calulation is the following (explained in renting):
cost=prob1*dist to station 1 + (1-Prob1)*prob2*dist to station 2 + ... where prob 1 and prob 2 are the probabilities to get a bike at those stations. 
The sum is completed up to a total probability of 1 - minimumMarginProbability.
When selecting the next station in the list a heuristic is used.

In this class, the special fact is that when renting, instead of distance, we use squared distance/maxdistance_recomendation. 
Thus, higher distances have higher cost. and lower have lower costs. by normalizing to maxdistance_recomendation
the cost is igual to the distance at maxdistance_recomendation. It increases higher above and is lower below.


a in renting instead of determining cost by prob*distance
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.ComplexRecommendationSystemTypes;

import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.worldentities.consultSystems.StationUtilityData;
import es.urjc.ia.bikesurbanfleets.worldentities.infraestructure.entities.Station;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author holger
 */
public class ComplexCostCalculator2 {

    //methods for cost calculations
    public ComplexCostCalculator2(double marginprob, double unsuccostrent, double unsuccostret,
            double penalfactorrent, double penalfactorret, double walkvel, double cycvel, double minsecondaryprob,
            double maxDistanceRecomendation) {
        minimumMarginProbability = marginprob;
        unsuccessCostRent = unsuccostrent;
        penalisationfactorrent = penalfactorrent;
        unsuccessCostReturn = unsuccostret;
        penalisationfactorreturn = penalfactorret;
        walkingVelocity=walkvel;
        cyclingVelocity=cycvel;
        minProbSecondaryRecommendation=minsecondaryprob;
        this.maxDistanceRecomendation=maxDistanceRecomendation;
        maxWalktime=this.maxDistanceRecomendation/walkingVelocity;

    }

    final double minimumMarginProbability;
    final double unsuccessCostRent;
    final double penalisationfactorrent;
    final double unsuccessCostReturn;
    final double penalisationfactorreturn;
    final double walkingVelocity;
    final double cyclingVelocity;
    final double minProbSecondaryRecommendation;
    final double maxCostValue=5000;
    final double maxDistanceRecomendation;
    final double maxWalktime;

    private double getSqarewalktime(double walktime, double accwalktime) {
        double aux=(walktime+accwalktime);
        double aux2=aux*aux;
        return (aux2-(accwalktime*accwalktime))/maxWalktime;      
    }
    private double calculateWayCostRentHeuristic(List<StationUtilityData> way, StationUtilityData sd, double takeprob,
            double margprob, double walktime,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start,double accwalktime) {
        way.add(sd);
        double sqwalktime=getSqarewalktime(walktime,accwalktime);
        double thiscost= (margprob - minimumMarginProbability) * sqwalktime;
        double newmargprob = margprob * (1 - takeprob);
        double newaccwalktime=accwalktime+walktime;
        if (margprob <= minimumMarginProbability) {
            throw new RuntimeException("error parameters");
        }
        if (newmargprob <= minimumMarginProbability) {
            return thiscost;
        }
        //find best neighbour
        lookedlist.add(sd);
        StationUtilityData closestneighbour = bestNeighbourRent(sd.getStation(), newmargprob, lookedlist, allstats,newaccwalktime);
        double margcost;
        double extrastationpenalizationcost=(newmargprob-minimumMarginProbability) * unsuccessCostRent;
        if (closestneighbour!=null) {
            double newtime = sd.getStation().getPosition().distanceTo(closestneighbour.getStation().getPosition())/ walkingVelocity;
            if (start) {
                sd.bestNeighbour = closestneighbour;
            }
            margcost = extrastationpenalizationcost
                + calculateWayCostRentHeuristic(way, closestneighbour, closestneighbour.getProbabilityTake(), newmargprob, newtime, lookedlist, allstats, false,newaccwalktime);
        } else {
            margcost = extrastationpenalizationcost+
                    (newmargprob - minimumMarginProbability) * getSqarewalktime(newaccwalktime,maxCostValue);
        }
        return thiscost + penalisationfactorrent * margcost;
    }

    public double calculateWayCostRentHeuristic(List<StationUtilityData> way, StationUtilityData sd,
            double margprob, double walktime,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        return calculateWayCostRentHeuristic(way, sd, sd.getProbabilityTake(),
            margprob, walktime, lookedlist, allstats, start,0);
    }
    
    public double calculateCostRentHeuristic(StationUtilityData sd,
            double margprob, double walktime,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        return calculateWayCostRentHeuristic(new ArrayList<>(), sd, sd.getProbabilityTake(),
            margprob, walktime, lookedlist, allstats, start,0);
    }
    
 
    public double calculateCostRentHeuristic(StationUtilityData sd, double takeprob,
            double margprob, double currenttime,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        return calculateWayCostRentHeuristic(new ArrayList<>(), sd, takeprob,
            margprob, currenttime, lookedlist, allstats, start, 0);
    }

    //DO NOT CHANGE IT IS WORKING :)
    public double calculateWayCostReturnHeuristic(List<StationUtilityData> way, StationUtilityData sd, double returnprob,
            double margprob, double biketime, GeoPoint destination,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        way.add(sd);
        double thisbikecost = (margprob - minimumMarginProbability) * biketime;
        double thiswalktime = sd.getStation().getPosition().distanceTo(destination)/ walkingVelocity;
        double newmargprob = margprob * (1 - returnprob);
        double thistotalcost; 
        if (margprob <= minimumMarginProbability) {
            throw new RuntimeException("error parameters");
        }
        if (newmargprob <= minimumMarginProbability) {
            thistotalcost = thisbikecost + thiswalktime * (margprob - minimumMarginProbability);
            return thistotalcost;
        } else {
            thistotalcost = thisbikecost + thiswalktime * margprob * returnprob;
        }
        lookedlist.add(sd);
        StationUtilityData closestneighbour = bestNeighbourReturn(sd.getStation(), newmargprob, lookedlist, allstats, destination);
        double newbiketime = sd.getStation().getPosition().distanceTo(closestneighbour.getStation().getPosition()) / cyclingVelocity;
        if (start) {
            sd.bestNeighbour = closestneighbour;
        }
        double margvalue = newmargprob * unsuccessCostReturn
                + calculateWayCostReturnHeuristic(way, closestneighbour, closestneighbour.getProbabilityReturn(), newmargprob, newbiketime, destination, lookedlist, allstats, false);
        return thistotalcost + penalisationfactorreturn * margvalue;
    }
 
    public double calculateWayCostReturnHeuristic(List<StationUtilityData> way, StationUtilityData sd,
            double margprob, double biketime, GeoPoint destination,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
    return calculateWayCostReturnHeuristic(way, sd, sd.getProbabilityReturn(),
            margprob, biketime, destination,lookedlist,allstats,  start);
    }

       //DO NOT CHANGE IT IS WORKING :)
    public double calculateCostReturnHeuristic(StationUtilityData sd,
            double margprob, double biketime, GeoPoint destination,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
    return calculateWayCostReturnHeuristic(new ArrayList<>(), sd, sd.getProbabilityReturn(),
            margprob, biketime, destination,lookedlist,allstats,  start);
    }
    
    public double calculateCostReturnHeuristic(StationUtilityData sd, double returnprob,
            double margprob, double biketime, GeoPoint destination,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
    return calculateWayCostReturnHeuristic(new ArrayList<>(), sd, returnprob,
            margprob, biketime, destination,lookedlist,allstats,  start);
    }
   
//alternative ways for calculation but with distance not with time
/*        //DO NOT CHANGE IT IS WORKING :)
    private double calculateCostRent_best(StationUtilityData sd,
            double margprob, double accdist,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        double prob = sd.getProbabilityTake();
        double newmargprob = margprob * (1 - prob);
        if (margprob <= minimumMarginProbability) {
            throw new RuntimeException("error parameters");
        }
        if (newmargprob <= minimumMarginProbability) {
            return (1 - minimumMarginProbability / margprob) * accdist;
        }
        //find best neighbour
        lookedlist.add(sd);
        StationUtilityData closestneighbour = bestNeighbourRent(sd.getStation(), newmargprob, lookedlist, allstats);
        double newdist = sd.getStation().getPosition().distanceTo(closestneighbour.getStation().getPosition());
        double newaccdist = accdist + newdist;
        if (start) {
            sd.bestNeighbour=closestneighbour;
        }
        double margcost = calculateCostRent_best(closestneighbour, newmargprob, newaccdist, lookedlist, allstats, false);
        return prob * accdist
                + this.parameters.penalisationfactorrent * (1 - prob) * (margcost + this.parameters.unsucesscostRent);
    }

    //DO NOT CHANGE IT IS WORKING :)
    private double calculateCostReturn_best(StationUtilityData sd,
            double margprob, double accbikedist, GeoPoint destination,
            List<StationUtilityData> lookedlist,
            List<StationUtilityData> allstats, boolean start) {
        double prob = sd.getProbabilityReturn();
        double newmargprob = margprob * (1 - prob);
        double walkdist = sd.getStation().getPosition().distanceTo(destination);
        if (margprob <= minimumMarginProbability) {
            throw new RuntimeException("error parameters");
        }
        if (newmargprob <= minimumMarginProbability) {
            return (1 - minimumMarginProbability / margprob) * accbikedist
                    + walkdist * (margprob - minimumMarginProbability) / margprob;
        }
        //find best neighbour
        lookedlist.add(sd);
        StationUtilityData closestneighbour = bestNeighbourReturn(sd.getStation(), newmargprob, lookedlist, allstats, destination);
        double newdist = sd.getStation().getPosition().distanceTo(closestneighbour.getStation().getPosition()) * this.parameters.bikefactor;
        if (start) {
            sd.bestNeighbour = closestneighbour;
        }
        double newaccbikedist = accbikedist + newdist;
        double margcost = calculateCostReturn_best(closestneighbour, newmargprob, newaccbikedist, destination, lookedlist, allstats, false);
        return prob * (accbikedist + walkdist)
                + this.parameters.penalisationfactorreturn * (1 - prob) * (margcost + this.parameters.unsucesscostReturn);
    }
*/
    private StationUtilityData bestNeighbourRent(Station s, double newmargprob, List<StationUtilityData> lookedlist, List<StationUtilityData> allstats,
            double accwalktime) {
        double newbestValueFound = Double.MAX_VALUE;
        StationUtilityData bestneighbour = null;
        for (StationUtilityData nei : allstats) {
            if (!lookedlist.contains(nei) && nei.getProbabilityTake() > minProbSecondaryRecommendation) {
                double newtime=s.getPosition().distanceTo(nei.getStation().getPosition())/ walkingVelocity ;
                double sqwalktime=getSqarewalktime(newtime,accwalktime);
                double altthiscost = newmargprob * sqwalktime;
                double altnewmargprob = newmargprob * (1 - nei.getProbabilityTake());
                altthiscost = altthiscost + altnewmargprob * getSqarewalktime(accwalktime+newtime,maxCostValue);
                if (altthiscost < newbestValueFound) {
                    newbestValueFound = altthiscost;
                    bestneighbour = nei;
                }
            }
        }
        return bestneighbour;
    }

    private StationUtilityData bestNeighbourReturn(Station s, double newmargprob, List<StationUtilityData> lookedlist, List<StationUtilityData> allstats, GeoPoint destination) {
        double newbestValueFound = Double.MAX_VALUE;
        StationUtilityData bestneighbour = null;
        for (StationUtilityData nei : allstats) {
            if (!lookedlist.contains(nei) && nei.getProbabilityReturn() > minProbSecondaryRecommendation) {
                double altthisbiketime = s.getPosition().distanceTo(nei.getStation().getPosition()) / cyclingVelocity;
                double altthisbikecost = newmargprob * altthisbiketime;
                double altthiswalktime = nei.getStation().getPosition().distanceTo(destination) / walkingVelocity;
                double altnewmargprob = newmargprob * (1 - nei.getProbabilityReturn());
                double alttotalcost = altthisbikecost + altthiswalktime * newmargprob * nei.getProbabilityReturn()
                            + altnewmargprob * maxCostValue;
                if (alttotalcost < newbestValueFound) {
                    newbestValueFound = alttotalcost;
                    bestneighbour = nei;
                }
            }
        }
        return bestneighbour;
    }
    
    //global cost calculation. calculates the cost of taking/returning and also the cost differences
    // returns the global costs
    public double calculateCostsRentAtStation(StationUtilityData sd,
            List<StationUtilityData> allstats, 
            double demandfactor, UtilitiesForRecommendationSystems urs) {
        //takecosts
        List<StationUtilityData> lookedlist = new ArrayList<>();
        List<StationUtilityData> way = new LinkedList<StationUtilityData>();
        double usercosttake = calculateWayCostRentHeuristic(way, sd , 1, sd.getWalkTime(), lookedlist, allstats, true);

        //analyze global costs
        double margprob = 1;
        double acctakecost = 0;
        double accreturncost = 0;
        int timeoffset = (int)sd.getWalkTime();
        lookedlist.clear();
        List<StationUtilityData> newlookedlist = new ArrayList<>();
        for (StationUtilityData wp : way) {
            //calculate takecost difference
            newlookedlist=new ArrayList<>(lookedlist);
            double costtake = calculateCostRentHeuristic(wp, 1, 0, newlookedlist, allstats, false);
            newlookedlist=new ArrayList<>(lookedlist);
            double costtakeafter = calculateCostRentHeuristic(wp, wp.getProbabilityTakeAfterTake(), 1, 0, newlookedlist, allstats, false);
            double difcosttake=(costtakeafter - costtake) ;
            //calculate return cost difference
            GeoPoint hipodestination = wp.getStation().getPosition();
            newlookedlist=new ArrayList<>(lookedlist);
            double costreturnhip = calculateCostReturnHeuristic(wp, 1, 0, hipodestination, newlookedlist, allstats, false);
            newlookedlist=new ArrayList<>(lookedlist);
            double costreturnafterhip = calculateCostReturnHeuristic(wp, wp.getProbabilityReturnAfterTake(), 1, 0, hipodestination, newlookedlist, allstats, false);
            double difcostreturn=(costreturnafterhip - costreturnhip) ;

            if (difcostreturn>0 || difcosttake<0){
                    System.out.println("EEEEERRRRROOOOORRRR: invalid cost station " + sd.getStation().getId() +  " " + difcosttake+ " " + difcostreturn );
            }
            //normalize costdiferences to demand
            double futtakedemand = urs.getFutureBikeDemand(wp.getStation(), timeoffset);
            double futreturndemand = urs.getFutureSlotDemand(wp.getStation(), timeoffset);
            double futglobaltakedem = urs.getFutureGlobalBikeDemand(timeoffset);
            double futglobalretdem = urs.getFutureGlobalSlotDemand(timeoffset);
            difcosttake = difcosttake *  futtakedemand * demandfactor;
            difcostreturn = difcostreturn* futreturndemand * demandfactor;

            //accumulate the costs based on the probability of returning/taking at station wp
            double newmargprob = margprob * (1 - wp.getProbabilityTake());
            if (margprob <= minimumMarginProbability) {
                throw new RuntimeException("error parameters");
            }
            if (newmargprob <= minimumMarginProbability) {
                acctakecost+= (margprob - minimumMarginProbability)*difcosttake;
                accreturncost+= (margprob - minimumMarginProbability)*difcostreturn;
            }
            else {
                acctakecost+= margprob * wp.getProbabilityTake() * difcosttake;
                accreturncost+= margprob * wp.getProbabilityTake() * difcostreturn;
                margprob=newmargprob;
            }
            lookedlist.add(wp);
        }

        double globalcost = usercosttake + acctakecost + accreturncost;
        sd.setIndividualCost(usercosttake).setTakecostdiff(acctakecost).setReturncostdiff(accreturncost);
        return globalcost;

    }

    public double calculateCostsReturnAtStation(StationUtilityData sd, GeoPoint destination,
            List<StationUtilityData> allstats, double demandfactor, UtilitiesForRecommendationSystems urs) {
        //return costs
        //take a close point to the station as hipotetical detsination
        List<StationUtilityData> lookedlist = new ArrayList<>();
        List<StationUtilityData> way = new LinkedList<StationUtilityData>();
        double usercostreturn = calculateWayCostReturnHeuristic(way, sd, 1, sd.getBiketime(), destination, lookedlist, allstats, true);

        //analyze global costs
        double margprob = 1;
        double acctakecost = 0;
        double accreturncost = 0;
        int timeoffset = (int) (sd.getBiketime());
        lookedlist.clear();
        List<StationUtilityData> newlookedlist = new ArrayList<>();
        for (StationUtilityData wp : way) {
            //calculate takecost difference
            newlookedlist=new ArrayList<>(lookedlist);
            double costtake = calculateCostRentHeuristic(wp, 1, 0, newlookedlist, allstats, false);
            newlookedlist=new ArrayList<>(lookedlist);
            double costtakeafter = calculateCostRentHeuristic( wp, wp.getProbabilityTakeAfterRerturn(), 1, 0, newlookedlist, allstats, false);
            double difcosttake=(costtakeafter - costtake) ;
            //calculate return cost difference
            GeoPoint hipodestination = wp.getStation().getPosition();
            newlookedlist=new ArrayList<>(lookedlist);
            double costreturnhip = calculateCostReturnHeuristic(wp, 1, 0, hipodestination, newlookedlist, allstats, false);
            newlookedlist=new ArrayList<>(lookedlist);
            double costreturnafterhip = calculateCostReturnHeuristic(wp, wp.getProbabilityReturnAfterReturn(), 1, 0, hipodestination, newlookedlist, allstats, false);
            double difcostreturn=(costreturnafterhip - costreturnhip) ;

            if (difcostreturn<0 || difcosttake>0){
                    System.out.println("EEEEERRRRROOOOORRRR: invalid cost station in return  " + sd.getStation().getId() +  " " + difcosttake+ " " + difcostreturn );
            }

            //normalize costdiferences to demand
            double futtakedemand = urs.getFutureBikeDemand(wp.getStation(), timeoffset);
            double futreturndemand = urs.getFutureSlotDemand(wp.getStation(), timeoffset);
            double futglobaltakedem = urs.getFutureGlobalBikeDemand(timeoffset);
            double futglobalretdem = urs.getFutureGlobalSlotDemand(timeoffset);
            difcosttake = difcosttake* futtakedemand * demandfactor;
            difcostreturn = difcostreturn* futreturndemand * demandfactor;

            //accumulate the costs based on the probability of returning/taking at station wp
            double newmargprob = margprob * (1 - wp.getProbabilityReturn());
            if (margprob <= minimumMarginProbability) {
                throw new RuntimeException("error parameters");
            }
            if (newmargprob <= minimumMarginProbability) {
                acctakecost+= (margprob - minimumMarginProbability)*difcosttake;
                accreturncost+= (margprob - minimumMarginProbability)*difcostreturn;
            }
            else {
                acctakecost+= margprob * wp.getProbabilityReturn() * difcosttake;
                accreturncost+= margprob * wp.getProbabilityReturn() * difcostreturn;
                margprob=newmargprob;
            }
            lookedlist.add(wp);
        }

        double globalcost = usercostreturn + acctakecost + accreturncost;
        sd.setIndividualCost(usercostreturn).setTakecostdiff(acctakecost).setReturncostdiff(accreturncost);
        return globalcost;
    }

}
