/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.resultanalysis;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opencsv.CSVWriter;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoPoint;
import es.urjc.ia.bikesurbanfleets.common.graphs.GeoRoute;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphHopperIntegration;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphManager;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GeoRouteCreationException;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GraphHopperIntegrationException;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.history.HistoryJsonClasses;
import es.urjc.ia.bikesurbanfleets.history.HistoryJsonClasses.FinalGlobalValues;
import es.urjc.ia.bikesurbanfleets.history.entities.HistoricStation;
import es.urjc.ia.bikesurbanfleets.history.entities.HistoricUser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author holger
 */
public class SimulationResultAnalyser {

    double standardstraightLineWalkingVelocity = 1.4 / GeoPoint.STRAIGT_LINE_FACTOR;
    double standardstraightLineCyclingVelocity = 6 / GeoPoint.STRAIGT_LINE_FACTOR;

    public static void main(String[] args) throws Exception {
        String historydir = "/Users/holger/workspace/BikeProjects/Bike3S/Bike3STests/tests600max/fasttest/1/history/2/";
        String analysisdir = "/Users/holger/workspace/BikeProjects/Bike3S/Bike3STests/tests600max/fasttest/1/analysis/2/";
        String tempdir = System.getProperty("user.home") + "/.Bike3S";
        String mapdir = "/Users/holger/workspace/BikeProjects/Bike3S/madrid.osm";

        GraphManager gm = new GraphHopperIntegration(mapdir, tempdir);
        SimulationResultAnalyser sa = new SimulationResultAnalyser(analysisdir, historydir, gm);
        sa.analyzeSimulation();
    }
    private static Gson gson = new GsonBuilder()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .setPrettyPrinting()
            .create();

    class UserMetric {

        int timeapp = -1;
        int timegetbike = -1;
        int timeretbike = -1;
        int timeleafe = -1;
        Event.RESULT_TYPE leafreason;
        int succbikereservations = 0;
        int failedbikereservations = 0;
        int succslotreservations = 0;
        int failesslotreservations = 0;
        int succbikerentals = 0;
        int failedbikerentals = 0;
        int succbikereturns = 0;
        int failedbaikereturns = 0;
        boolean finishedinsimtime = false;
        GeoPoint origin;
        GeoPoint destination;
        double walkvelocity;
        double cyclevelocity;
        double additionaltimeloss = 0;
        double artificialtime = 0;
        GeoPoint lastposition;
    }

    class StationMetric {

        GeoPoint position;
        int id;
        int succbikereservations = 0;
        int failedbikereservations = 0;
        int succslotreservations = 0;
        int failesslotreservations = 0;
        int succbikerentals = 0;
        int failedbikerentals = 0;
        int succbikereturns = 0;
        int failedbaikereturns = 0;
        double balancingquality = 0D;
        long emtytime = 0;
        int timelastchange = 0;
        int currentavbikes = 0;
        int capacity;
    }
    TreeMap<Integer, StationMetric> stationmetrics = new TreeMap<Integer, StationMetric>();
    TreeMap<Integer, UserMetric> usermetrics = new TreeMap<Integer, UserMetric>();

    private Path analysispath;
    private Path historypath;
    private int totalsimtimespecified = 0;
    int stations_num;
    int stations_numerwithEmptytimes = 0;
    double stations_av_emptytime_stations = 0;
    double stations_av_equilibrium = 0;
    int users_num;
    int users_finishedInSimTime = 0;
    double users_av_tostationtime = 0;
    double users_av_biketime = 0;
    double users_av_todesttime = 0;
    double users_DS = 0;
    double users_HE = 0;
    double users_RE = 0;
    int users_numabandon = 0;
    TreeMap<Integer, Integer> users_takefails = new TreeMap<>();
    TreeMap<Integer, Integer> users_returnfails = new TreeMap<>();
    int users_totalrentalfail = 0;
    int users_totalreturnfail = 0;
    double users_av_additionaltimeloss = 0D;
    GraphManager routeService;

    public SimulationResultAnalyser(String analysisdir, String historydir, GraphManager routeService) throws IOException {
        this.analysispath = Paths.get(analysisdir);
        this.historypath = Paths.get(historydir);
        this.routeService = routeService;
    }

    public SimulationResultAnalyser(String analysisdir, String historydir) throws IOException {
        this.analysispath = Paths.get(analysisdir);
        this.historypath = Paths.get(historydir);
        this.routeService = null;
    }

    public void analyzeSimulation() throws IOException, GraphHopperIntegrationException, GeoRouteCreationException {
        // read global values
        File json = historypath.resolve("final-global-values.json").toFile();
        FileReader red = new FileReader(json);
        FinalGlobalValues fgv = gson.fromJson(red, FinalGlobalValues.class);
        totalsimtimespecified = fgv.getTotalTimeSimulation();
        red.close();

        // setup metrics
        stationmetrics = new TreeMap<Integer, StationMetric>();
        usermetrics = new TreeMap<Integer, UserMetric>();

        //preprocess data (read initial station info
        preprocess();

        //read history
        File file = new File(historypath.toString());
        File[] directories = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isFile();
            }
        });
        LinkedList<File> filelist = new LinkedList<File>();
        for (File histname : directories) {
            if (histname.getName().matches("\\d+\\-\\d+\\_\\d+\\.json")) {
                int i = 0;
                int valuen = Integer.parseInt(histname.getName().substring(0, histname.getName().indexOf('-')));
                for (i = 0; i < filelist.size(); i++) {
                    int valueo = Integer.parseInt(filelist.get(i).getName().substring(0, filelist.get(i).getName().indexOf('-')));
                    if (valuen < valueo) {
                        break;
                    }
                }
                filelist.add(i, histname);
            }
        }
        for (File histname : filelist) {
            processHistoryEntries(readHistoryEntries(histname));
        }

        //write the results to files
        postprocess();
        File auxiliaryDir = analysispath.toFile();
        if (!auxiliaryDir.exists()) {
            auxiliaryDir.mkdirs();
        }
        WriteGeneraldata();
        WiteUserdata();
        WiteStationdata();
    }

    private Integer getUserID(Collection<HistoryJsonClasses.IdReference> ent) {
        for (HistoryJsonClasses.IdReference ref : ent) {
            if (ref.getType().equals("users")) {
                return (Integer) ref.getId();
            }
        }
        throw new RuntimeException("no user id found");
    }

    private Integer getStationID(Collection<HistoryJsonClasses.IdReference> ent) {
        for (HistoryJsonClasses.IdReference ref : ent) {
            if (ref.getType().equals("stations")) {
                return (Integer) ref.getId();
            }
        }
        return null;
    }

    private void analyzeUserData(Map<String, List<JsonObject>> newEntities, String name, Integer userid, Integer stationId, int time, Event.RESULT_TYPE result) {
        //check new user appearance first
        if (name.equals("EventUserAppears")) {
            UserMetric newUM = new UserMetric();
            if (null != usermetrics.put(userid, newUM)) {
                throw new RuntimeException("user should not exist in historyanalysis");
            }
            if (newEntities == null) {
                throw new RuntimeException("user not found in new entities");
            }
            List<JsonObject> users = newEntities.get("users");
            if (users == null) {
                throw new RuntimeException("user not found in new entities");
            }
            HistoricUser hu = gson.fromJson(users.get(0), HistoricUser.class);
            if (userid != hu.getId()) {
                throw new RuntimeException("user not found in new entities");
            }

            //now copy the user data:
            newUM.origin = hu.getPosition();
            newUM.destination = hu.getDestinationLocation();
            newUM.walkvelocity = hu.getWalkingVelocity();
            newUM.cyclevelocity = hu.getCyclingVelocity();
            newUM.timeapp = time;
            newUM.lastposition = newUM.origin;
            return;
        }

        // in any other case the user should exist
        UserMetric usM = usermetrics.get(userid);
        StationMetric stM = null;
        if (stationId != null) {
            stM = stationmetrics.get(stationId);
        }
        switch (name) {
            case "EventUserArrivesAtStationToRentBike":
                usM.artificialtime += (usM.lastposition.distanceTo(stM.position) / standardstraightLineWalkingVelocity);
                usM.lastposition = stM.position;
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RENTAL) {
                    usM.succbikerentals++;
                    usM.timegetbike = time;
                    //user rents bike
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RENTAL) {
                    usM.failedbikerentals++;
                }
                break;
            case "EventUserArrivesAtStationToReturnBike":
                usM.artificialtime += (usM.lastposition.distanceTo(stM.position) / standardstraightLineCyclingVelocity);
                usM.lastposition = stM.position;
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RETURN) {
                    usM.succbikereturns++;
                    usM.timeretbike = time;
                    //user returns bike
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RETURN) {
                    usM.failedbaikereturns++;
                }
                break;
            case "EventUserTriesToReserveSlot":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_SLOT_RESERVATION) {
                    usM.succslotreservations++;
                } else if (result == Event.RESULT_TYPE.FAILED_SLOT_RESERVATION) {
                    usM.failesslotreservations++;
                }
                break;
            case "EventUserTriesToReserveBike":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RESERVATION) {
                    usM.succbikereservations++;
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RESERVATION) {
                    usM.failedbikereservations++;
                }
                break;
            case "EventUserLeavesSystem":
                usM.artificialtime += (usM.lastposition.distanceTo(usM.destination) / standardstraightLineWalkingVelocity);
                usM.lastposition = usM.destination;
                usM.leafreason = result;
                if (time<=totalsimtimespecified) {
                    usM.finishedinsimtime = true;
                }
                //user leafs
                usM.timeleafe = time;
                break;
            default:
                break;
        }

    }

    private void analyzeStationData(String name, StationMetric stM, Event.RESULT_TYPE result) {

        switch (name) {
            case "EventUserArrivesAtStationToRentBike":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RENTAL) {
                    stM.succbikerentals++;
                    //user rents bike
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RENTAL) {
                    stM.failedbikerentals++;
                }
                break;
            case "EventUserArrivesAtStationToReturnBike":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RETURN) {
                    stM.succbikereturns++;
                    //user returns bike
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RETURN) {
                    stM.failedbaikereturns++;
                }
                break;
            case "EventUserTriesToReserveSlot":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_SLOT_RESERVATION) {
                    stM.succslotreservations++;
                } else if (result == Event.RESULT_TYPE.FAILED_SLOT_RESERVATION) {
                    stM.failesslotreservations++;
                }
                break;
            case "EventUserTriesToReserveBike":
                if (result == Event.RESULT_TYPE.SUCCESSFUL_BIKE_RESERVATION) {
                    stM.succbikereservations++;
                } else if (result == Event.RESULT_TYPE.FAILED_BIKE_RESERVATION) {
                    stM.failedbikereservations++;
                }
                break;
            default:
                break;
        }
    }

    private void processHistoryEntries(HistoryJsonClasses.TimeEntry[] historyentries) {

        //find the entries
        for (HistoryJsonClasses.TimeEntry historyentry : historyentries) {
            int time = historyentry.getTime();

            for (HistoryJsonClasses.EventEntry ee : historyentry.getEvents()) {
                Event.RESULT_TYPE result = ee.getResult();
                String name = ee.getName();
                Collection<HistoryJsonClasses.IdReference> involvedEntities = ee.getInvolvedEntities();
                Integer userid = getUserID(involvedEntities);
                Integer stationid = getStationID(involvedEntities);

                analyzeUserData(ee.getNewEntities(), name, userid, stationid, time, result);
 
                //station changes are inly considered up to totalsimulationtime
                if (time <= totalsimtimespecified && stationid!=null) {
                    StationMetric sm=stationmetrics.get(stationid);
                    checkChangesAvBikes(ee.getChanges(), time, sm);
                    analyzeStationData(name, sm, result);
                }
             }
        }
    }

    private void postprocess() throws GraphHopperIntegrationException, GeoRouteCreationException {
        //add the rest times for the stations
        stations_numerwithEmptytimes = 0;
        stations_av_emptytime_stations = 0;
        stations_av_equilibrium = 0;
        stations_num = stationmetrics.size();
        for (StationMetric sm : stationmetrics.values()) {
            if (sm.timelastchange > totalsimtimespecified) {
                throw new RuntimeException("some error");
            }
            double time = totalsimtimespecified - sm.timelastchange;
            if (sm.currentavbikes == 0) {
                sm.emtytime += time;
            }
            sm.balancingquality += Math.abs((double) sm.currentavbikes - ((double) sm.capacity / 2D)) * (double) time;
            sm.balancingquality = sm.balancingquality / totalsimtimespecified;
            if (sm.emtytime > 0) {
                stations_numerwithEmptytimes++;
            }
            stations_av_emptytime_stations += sm.emtytime;
            stations_av_equilibrium += sm.balancingquality;
        }
        stations_av_emptytime_stations = stations_av_emptytime_stations / (60D * (double) stations_num);
        stations_av_equilibrium = stations_av_equilibrium / (double) stations_num;

        //      System.out.println("in postprocess");
        //globval values for users
        users_num = usermetrics.size();
        users_finishedInSimTime = 0;
        users_av_tostationtime = 0;
        users_av_biketime = 0;
        users_av_todesttime = 0;
        users_takefails = new TreeMap<>();
        users_returnfails = new TreeMap<>();
        users_av_additionaltimeloss = 0;
        users_totalrentalfail = 0;
        users_totalreturnfail = 0;
        int succusers = 0;
        int i = 0;
        int printi = 0;
        for (UserMetric um : usermetrics.values()) {
            if (um.finishedinsimtime) {
                users_finishedInSimTime++;
                if (um.timegetbike >= 0) {//time counts if user did not abandom
                    succusers++;
                    users_av_tostationtime += um.timegetbike - um.timeapp;
                    users_av_biketime += um.timeretbike - um.timegetbike;
                    users_av_todesttime += um.timeleafe - um.timeretbike;
                    users_totalrentalfail += um.failedbikerentals;
                    users_totalreturnfail += um.failedbaikereturns;
                    Integer old = users_takefails.get(um.failedbikerentals);
                    if (old == null) {
                        users_takefails.put(um.failedbikerentals, 1);
                    } else {
                        users_takefails.put(um.failedbikerentals, old + 1);
                    }
                    old = users_returnfails.get(um.failedbaikereturns);
                    if (old == null) {
                        users_returnfails.put(um.failedbaikereturns, 1);
                    } else {
                        users_returnfails.put(um.failedbaikereturns, old + 1);
                    }
                    addTimeComparison(um);
                    users_av_additionaltimeloss += um.additionaltimeloss;
                }
                if (um.leafreason != Event.RESULT_TYPE.EXIT_AFTER_REACHING_DESTINATION) {
                    users_numabandon++;
                }
            }
        }
        if (users_finishedInSimTime != succusers + users_numabandon) {
            throw new RuntimeException("something wrong in data analsysis");
        }
        users_av_additionaltimeloss = users_av_additionaltimeloss / ((double) succusers * 60D);
        users_av_tostationtime = users_av_tostationtime / ((double) succusers * 60D);
        users_av_biketime = users_av_biketime / ((double) succusers * 60D);
        users_av_todesttime = users_av_todesttime / ((double) succusers * 60D);
        users_DS = (double) succusers / (double) users_finishedInSimTime;
        users_HE = (double) succusers / ((double) users_totalrentalfail + succusers);
        users_RE = (double) succusers / ((double) users_totalreturnfail + succusers);
    }

    private void preprocess() throws IOException {
        File stfile = historypath.resolve("entities/stations.json").toFile();
        FileReader r = new FileReader(stfile);
        JsonObject jo = gson.fromJson(new FileReader(stfile), JsonObject.class);
        JsonArray st = jo.getAsJsonArray("instances");
        for (JsonElement je : st) {
            HistoricStation hs = gson.fromJson(je, HistoricStation.class);
            Integer id = hs.getId();
            StationMetric sm = new StationMetric();
            sm.capacity = hs.getCapacity();
            sm.currentavbikes = hs.getAvailablebikes();
            sm.position = hs.getPosition();
            sm.id = id;
            if (stationmetrics.put(id, sm) != null) {
                throw new RuntimeException("duplicate station");
            }
        }
        r.close();
    }

    private void checkChangesAvBikes(Map<String, List<JsonObject>> changes, int currenttime, StationMetric sm) {
        if (changes == null) {
            return;
        }
        List<JsonObject> stationch = changes.get("stations");
        if (stationch == null) {
            return;
        }
        for (JsonObject o : stationch) {
            int id = o.get("id").getAsInt();
            if (sm.id != id) {
                throw new RuntimeException("station not in involved entities but in changes");
            }
            JsonObject o1 = o.getAsJsonObject("availablebikes");
            if (o1 != null) {
                int oldv = o1.get("old").getAsInt();
                int newv = o1.get("new").getAsInt();
                if (sm.currentavbikes != oldv) {
                    throw new RuntimeException("invalid value");
                }
                double time = currenttime - sm.timelastchange;
                if (sm.currentavbikes == 0) {
                    sm.emtytime += time;
                }
                sm.balancingquality += Math.abs((double) sm.currentavbikes - ((double) sm.capacity / 2D)) * (double) time;
                sm.currentavbikes = newv;
                sm.timelastchange = currenttime;
            }
        }
    }

    private static HistoryJsonClasses.TimeEntry[] readHistoryEntries(File name) throws IOException {
        // it creates a file with the specified name in the history directory

        // itreads the specified content from the  file
        FileReader reader = new FileReader(name);
        HistoryJsonClasses.TimeEntry[] timeentries = gson.fromJson(reader, HistoryJsonClasses.TimeEntry[].class);
        reader.close();
        return timeentries;
    }

    private void WriteGeneraldata() throws IOException {
        File outfile = this.analysispath.resolve("global_values.csv").toFile();
        Writer writer = new FileWriter(outfile);
        CSVWriter csvWriter = new CSVWriter(writer,
                ';',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        //write empty line
        csvWriter.writeNext(new String[]{""});

        //first line statation data
        int maxrentfails = users_takefails.lastKey();
        int maxreturnfails = users_returnfails.lastKey();

        String[] record = new String[3];
        record[0] = "simulatiuon time (min)";
        record[1] = Double.toString((double) totalsimtimespecified / 60D);
        record[2] = "all global results are calculated up to this time (users not finishing up to this time are ignored)";
        csvWriter.writeNext(record);

        //write empty line
        csvWriter.writeNext(new String[]{""});

        //Now set the String array for writing
        record = new String[13 + maxrentfails + maxreturnfails + 2];

        //write header for user data
        record[0] = "#users total";
        record[1] = "#users finished in simulationtime";
        record[2] = "#abandoned";
        record[3] = "DS";
        record[4] = "HE";
        record[5] = "RE";
        record[6] = "Av. time to station (min)(only succesfull users)";
        record[7] = "Av. time from orig to dest station (min)(only succesfull users)";
        record[8] = "Av. time to final destination (min)(only succesfull users)";
        record[9] = "Av. total time (min)";
        record[10] = "Av. timeloss (min)";
        record[11] = "#failed rentals (only succesfull users)";
        record[12] = "#failed returns (only succesfull users)";
        int i = 0;
        while (i <= maxrentfails) {
            record[13 + i] = "# with " + i + " rental fails";
            i++;
        }
        int j = 0;
        while (j <= maxreturnfails) {
            record[13 + i + j] = "# with " + j + " return fails";
            j++;
        }
        csvWriter.writeNext(record);

        //write global user data
        record[0] = Integer.toString(users_num);
        record[1] = Integer.toString(users_finishedInSimTime);
        record[2] = Integer.toString(users_numabandon);
        record[3] = Double.toString(users_DS);
        record[4] = Double.toString(users_HE);
        record[5] = Double.toString(users_RE);
        record[6] = Double.toString(users_av_tostationtime);
        record[7] = Double.toString(users_av_biketime);
        record[8] = Double.toString(users_av_todesttime);
        record[9] = Double.toString(users_av_tostationtime + users_av_biketime + users_av_todesttime);
        record[10] = Double.toString(users_av_additionaltimeloss);
        record[11] = Integer.toString(users_totalrentalfail);
        record[12] = Integer.toString(users_totalreturnfail);
        for (Integer key : users_takefails.keySet()) {
            record[13 + key] = Integer.toString(users_takefails.get(key));
        }
        for (Integer key : users_returnfails.keySet()) {
            record[13 + maxrentfails + 1 + key] = Integer.toString(users_returnfails.get(key));
        }
        csvWriter.writeNext(record);

        //write empty line
        csvWriter.writeNext(new String[]{""});

        //now write sation summary
        record = new String[4];
        record[0] = "#stations";
        record[1] = "#stations with empty times";
        record[2] = "Av. empty times (min)";
        record[3] = "Av. equilibrium desviation (over stations and simulationtime)";
        csvWriter.writeNext(record);

        record[0] = Integer.toString(stations_num);
        record[1] = Integer.toString(stations_numerwithEmptytimes);
        record[2] = Double.toString(stations_av_emptytime_stations);
        record[3] = Double.toString(stations_av_equilibrium);
        csvWriter.writeNext(record);
        writer.close();
    }

    private void WiteUserdata() throws IOException {
        File outfile = this.analysispath.resolve("users.csv").toFile();
        Writer writer = new FileWriter(outfile);
        CSVWriter csvWriter = new CSVWriter(writer,
                ';',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        //Now set the String array for writing
        String[] record = new String[17];

        //write header
        record[0] = "id";
        record[1] = "finished in simulationtime";
        record[2] = "appearance time (sec form start)";
        record[3] = "get bike time (sec from start)";
        record[4] = "return bike time (sec from start)";
        record[5] = "at final destination (sec from start)";
        record[6] = "total time (min)";
        record[7] = "additional time used over best aproximate time (min)";
        record[8] = "exit reason";
        record[9] = "Successful bike reservations";
        record[10] = "Failed bike reservations";
        record[11] = "Successful slot reservations";
        record[12] = "Failed slot reservations";
        record[13] = "Successful bike rentals";
        record[14] = "Failed bike rentals";
        record[15] = "Successful bike returns";
        record[16] = "Failed bike returns";
        csvWriter.writeNext(record);

        //now write the user values
         for (Integer id : usermetrics.keySet()) {
            boolean aband=true;
            UserMetric um = usermetrics.get(id);
            record[0] = Integer.toString(id);
            record[2] = Integer.toString(um.timeapp);
            if (um.timegetbike >= 0) {
                aband=false;
            }
            if (um.finishedinsimtime) {
                record[1] = "yes";
             } else {
                record[1] = "no";
            }
            if (um.timegetbike >= 0) {
                record[3] = Integer.toString(um.timegetbike);
            } else {
                record[3] = "";
            }
            if (um.timeretbike >= 0) {
                record[4] = Integer.toString(um.timeretbike);
            } else {
                record[4] = "";
            }
            if (um.timegetbike >= 0) {
                record[5] = Integer.toString(um.timeleafe);
            } else {
                record[5] = "";
            }
            if (um.finishedinsimtime && !aband) {
                record[6] = Double.toString(((double) (um.timeleafe - um.timeapp)) / 60D);
                record[7] = Double.toString(((double) um.additionaltimeloss) / 60D);
            } else {
                record[6] = "";
                record[7] = "";
            }
            if (um.finishedinsimtime) record[8] = um.leafreason.toString();
            else record[8] = "";
            record[9] = Integer.toString(um.succbikereservations);
            record[10] = Integer.toString(um.failedbikereservations);
            record[11] = Integer.toString(um.succslotreservations);
            record[12] = Integer.toString(um.failesslotreservations);
            record[13] = Integer.toString(um.succbikerentals);
            record[14] = Integer.toString(um.failedbikerentals);
            record[15] = Integer.toString(um.succbikereturns);
            record[16] = Integer.toString(um.failedbaikereturns);
            //write line
            csvWriter.writeNext(record);
        }
        writer.close();
    }

    private void WiteStationdata() throws IOException {
        File outfile = this.analysispath.resolve("stations.csv").toFile();
        Writer writer = new FileWriter(outfile);
        CSVWriter csvWriter = new CSVWriter(writer,
                ';',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        //Now set the String array for writing
        String[] record = new String[11];

        //write header
        record[0] = "id";
        record[1] = "emptytime (min)";
        record[2] = "average deviation from halfcapacity";
        record[3] = "Successful bike reservations";
        record[4] = "Failed bike reservations";
        record[5] = "Successful slot reservations";
        record[6] = "Failed slot reservations";
        record[7] = "Successful bike rentals";
        record[8] = "Failed bike rentals";
        record[9] = "Successful bike returns";
        record[10] = "Failed bike returns";
        csvWriter.writeNext(record);

        //now write the station data
        for (Integer id : stationmetrics.keySet()) {
            StationMetric sm = stationmetrics.get(id);
            //postprocess information
            record[0] = Integer.toString(id);
            record[1] = Double.toString((double) sm.emtytime / 60D);
            record[2] = Double.toString(sm.balancingquality);
            record[3] = Integer.toString(sm.succbikereservations);
            record[4] = Integer.toString(sm.failedbikereservations);
            record[5] = Integer.toString(sm.succslotreservations);
            record[6] = Integer.toString(sm.failesslotreservations);
            record[7] = Integer.toString(sm.succbikerentals);
            record[8] = Integer.toString(sm.failedbikerentals);
            record[9] = Integer.toString(sm.succbikereturns);
            record[10] = Integer.toString(sm.failedbaikereturns);
            //write line
            csvWriter.writeNext(record);
        }
        writer.close();
    }

    //compare the shortes routetime with the actual routetime 
    //the shortes rute is calculated by specifying the closest station to the user position, the clostes station to the user destination 
    // and searching the rutepartes inbetween 
    private void addTimeComparison(UserMetric um) throws GraphHopperIntegrationException, GeoRouteCreationException {

        double closeststartdist = Double.MAX_VALUE;
        double closestenddist = Double.MAX_VALUE;
        StationMetric bestStartStation = null;
        StationMetric bestEndStation = null;
        for (StationMetric hs : stationmetrics.values()) {
            double currentdist = hs.position.distanceTo(um.origin);
            if (currentdist < closeststartdist) {
                closeststartdist = currentdist;
                bestStartStation = hs;
            }
            currentdist = hs.position.distanceTo(um.destination);
            if (currentdist < closestenddist) {
                closestenddist = currentdist;
                bestEndStation = hs;
            }
        }
        //Now get the routes
        double shortesttime = 0;
        if (routeService != null) {
            GeoRoute gr = routeService.obtainShortestRouteBetween(um.origin, bestStartStation.position, "foot");
            shortesttime += (int) (gr.getTotalDistance() / um.walkvelocity);
            gr = routeService.obtainShortestRouteBetween(bestStartStation.position, bestEndStation.position, "bike");
            shortesttime += (int) (gr.getTotalDistance() / um.cyclevelocity);
            gr = routeService.obtainShortestRouteBetween(bestEndStation.position, um.destination, "foot");
            shortesttime += (int) (gr.getTotalDistance() / um.walkvelocity);
            um.additionaltimeloss = (um.timeleafe - um.timeapp) - shortesttime;
        } else {

            shortesttime =  ((um.origin.distanceTo(bestStartStation.position) / this.standardstraightLineWalkingVelocity)
                    + (bestStartStation.position.distanceTo(bestEndStation.position) / this.standardstraightLineCyclingVelocity)
                    + (bestEndStation.position.distanceTo(um.destination) / this.standardstraightLineWalkingVelocity));
            um.additionaltimeloss = (int)(um.artificialtime - shortesttime);
        }
    }

}
