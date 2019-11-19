/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.services.demandManager;

import com.google.gson.JsonObject;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import static es.urjc.ia.bikesurbanfleets.common.util.ParameterReader.getParameters;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 *
 * @author holger
 */
@DemandManagerType("FileBasedDemandManager")
public class FileBasedDemandManager extends DemandManager{

    private static class DemandManParameters {
        private String demandDataFile=null;
    }

    DemandManParameters parameters=null;
    
    public FileBasedDemandManager(JsonObject parameterdef) throws Exception {
        super();
        this.parameters = new DemandManParameters();
        getParameters(parameterdef, this.parameters);
        ReadData(parameters.demandDataFile);
    }
    public FileBasedDemandManager(String demandDataFile) throws Exception {
        super();
        this.parameters = new DemandManParameters();
        parameters.demandDataFile=demandDataFile;
        ReadData(parameters.demandDataFile);
    }

    //if no demand data is available, the average of all stations in the same period is returned
    //this is the minimum demand that is assumed if teh obtained demand is 0
    private final static double MIN_DEMAND = 0.05;//0.05

    private static class DemandResult {

        boolean hasdemand;
        double demand;

        DemandResult(boolean hasdemand, double demand) {
            this.hasdemand = hasdemand;
            this.demand = demand;
        }
    }
    Demand dem = new Demand();

    @Override
    public double getStationTakeRateIntervall(int stationID, LocalDateTime start, double endtimeoffset) {
        return getRateIntervall(stationID, true, start, endtimeoffset);
    }
    public double getStationReturnRateIntervall(int stationID, LocalDateTime start, double endtimeoffset) {
        return getRateIntervall(stationID, false, start, endtimeoffset);
    }
    public double getGlobalTakeRateIntervall(LocalDateTime start, double endtimeoffset) {
        return getRateIntervall(-1000, true, start, endtimeoffset);
    }
    public double getGlobalReturnRateIntervall(LocalDateTime start, double endtimeoffset) {
        return getRateIntervall(-1000, false, start, endtimeoffset);
    }
    private double getRateIntervall(int stationID, boolean take, LocalDateTime start, double endtimeoffset) {
        if (endtimeoffset<0)
            throw new RuntimeException("Invalid enttimeparameter");
        double retdemand=0;
        double restsecs=endtimeoffset;
        LocalDateTime tempdateTime=start;
        while (restsecs>0) {
            //get the proportion of the demand in each hour
            double hourdemand;
            if(stationID==-1000) {
                if (take) hourdemand=getGlobalTakeRatePerHour(tempdateTime);
                else hourdemand=getGlobalReturnRatePerHour(tempdateTime);
            } else {
                if (take) hourdemand=getStationTakeRatePerHour(stationID,tempdateTime);
                else hourdemand=getStationReturnRatePerHour(stationID,tempdateTime);
            }
            long secsleftincurrenthour=(60-tempdateTime.getSecond()) + 
                    ((60-tempdateTime.getMinute()-1)*60);
            double adddemand=0;
            if (secsleftincurrenthour < restsecs) {
                adddemand=hourdemand*((double)secsleftincurrenthour/3600D);
                //setrestsecs and next hour
                restsecs-=secsleftincurrenthour;
                tempdateTime=tempdateTime.plusSeconds(secsleftincurrenthour);
            } else {
                adddemand=hourdemand*(restsecs/3600D);
                restsecs=0;
            }
            retdemand=retdemand+adddemand;
        }
        return retdemand;
    }

    public double getStationTakeRatePerHour(int stationID, LocalDateTime t) {
        return getStationTakeRatePerHour(stationID, Month.toDemandMangerMonth(t.getMonth()), WeekDay.toDemandMangerDay(t.getDayOfWeek()), t.getHour());
    }

    public double getStationReturnRatePerHour(int stationID, LocalDateTime t) {
        return getStationReturnRatePerHour(stationID, Month.toDemandMangerMonth(t.getMonth()), WeekDay.toDemandMangerDay(t.getDayOfWeek()), t.getHour());
    }

    public double getGlobalTakeRatePerHour(LocalDateTime t) {
        return getGlobalTakeRatePerHour(Month.toDemandMangerMonth(t.getMonth()), WeekDay.toDemandMangerDay(t.getDayOfWeek()), t.getHour());
    }

    public double getGlobalReturnRatePerHour(LocalDateTime t) {
        return getGlobalReturnRatePerHour(Month.toDemandMangerMonth(t.getMonth()), WeekDay.toDemandMangerDay(t.getDayOfWeek()), t.getHour());
    }

    public double getStationTakeRatePerHour(int stationID, Month month, WeekDay day, int hour) {
        DemandResult r = dem.getDemandStation(stationID, month, day, hour, true);
        double result;
        if (!r.hasdemand) {
            result = dem.getAverageStationDemand(month, day, hour, true);
        } else {
            result = r.demand;
        }
        if (result < MIN_DEMAND) {
            return MIN_DEMAND;
        }
        return result;
    }

    public double getStationReturnRatePerHour(int stationID, Month month, WeekDay day, int hour) {
        DemandResult r = dem.getDemandStation(stationID, month, day, hour, false);
        double result;
        if (!r.hasdemand) {
            result = dem.getAverageStationDemand(month, day, hour, false);
        } else {
            result = r.demand;
        }
        if (result < MIN_DEMAND) {
            return MIN_DEMAND;
        }
        return result;
    }

    public double getGlobalTakeRatePerHour(Month month, WeekDay day, int hour) {
        double result = dem.getDemandGlobal(month, day, hour, true);
        if (result < MIN_DEMAND) {
            return MIN_DEMAND;
        }
        return result;
    }

    @Override
    public double getGlobalReturnRatePerHour(Month month, WeekDay day, int hour) {
        double result = dem.getDemandGlobal(month, day, hour, false);
        if (result < MIN_DEMAND) {
            return MIN_DEMAND;
        }
        return result;
    }

    private void ReadData(String file) {
        String[] line = new String[1];
        try {
            FileReader filereader = new FileReader(file);
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(';')
                    //.withIgnoreQuotations(true)
                    //.withEscapeChar('@')
                    .build();
            CSVReader csvReader
                    = new CSVReaderBuilder(filereader)
                    .withSkipLines(1)
                    .withCSVParser(parser)
                    .build();
            while ((line = csvReader.readNext()) != null) {
                int station = Integer.parseInt(line[0]);
                int day = Integer.parseInt(line[1]);
                int month = Integer.parseInt(line[2]);
                int year = Integer.parseInt(line[3]);
                int hour = Integer.parseInt(line[4]);
                int takeNum = Integer.parseInt(line[5]);
                int returnNum = Integer.parseInt(line[6]);
                String dayOfWeek = line[7];
                /*     if (day==5 && month==10) {
                    System.out.println();
                    for (String s : line) {
                        System.out.print(s + " ");
                    }
                 */
                dem.add(station, month, dayOfWeek, hour, takeNum, returnNum);
                //     }
            }
            dem.setGlobalDemand();
        } catch (Exception ex) {
            throw new RuntimeException("error reading demand data");
        }
    }

    private static class Demand {

        HashMap< Integer, StationData> stationMap;
        HashMap< Month, HashMap<WeekDay, double[][]>> globalDemand;
        int numberstations;

        int getNumberStations() {
            return stationMap.size();
        }

        Demand() {
            stationMap = new HashMap< Integer, StationData>();
        }

        void add(int station, int month, String day, int hour, int take, int ret) {
            //first convert day and month
            WeekDay d;
            Month m;
            if (day.equals("lun")) {
                d = WeekDay.Mon;
            } else if (day.equals("mar")) {
                d = WeekDay.Tue;
            } else if (day.equals("mie")) {
                d = WeekDay.Wed;
            } else if (day.equals("jue")) {
                d = WeekDay.Thu;
            } else if (day.equals("vie")) {
                d = WeekDay.Fri;
            } else if (day.equals("sab")) {
                d = WeekDay.Sat;
            } else if (day.equals("dom")) {
                d = WeekDay.Sun;
            } else {
                throw new RuntimeException("invalid day text:" + day);
            }
            if (month == 1) {
                m = Month.Jan;
            } else if (month == 2) {
                m = Month.Feb;
            } else if (month == 3) {
                m = Month.Mar;
            } else if (month == 4) {
                m = Month.Apr;
            } else if (month == 5) {
                m = Month.May;
            } else if (month == 6) {
                m = Month.Jun;
            } else if (month == 7) {
                m = Month.Jul;
            } else if (month == 8) {
                m = Month.Aug;
            } else if (month == 9) {
                m = Month.Sep;
            } else if (month == 10) {
                m = Month.Oct;
            } else if (month == 11) {
                m = Month.Nov;
            } else if (month == 12) {
                m = Month.Dic;
            } else {
                throw new RuntimeException("invalid month:" + month);
            }
            if (hour < 0 || hour > 23) {
                throw new RuntimeException("invalid hour:" + hour);
            }
            //now add to station
            StationData sd = stationMap.get(station);
            if (sd == null) {
                sd = new StationData();
                stationMap.put(station, sd);
            }
            sd.add(m, d, hour, take, ret);
        }

        void setGlobalDemand() {
            globalDemand = new HashMap< Month, HashMap<WeekDay, double[][]>>(15);
            for (int key : stationMap.keySet()) {
                StationData stationdata = stationMap.get(key);

                for (Month stationmonth : stationdata.monthMap.keySet()) {
                    MonthData stationmonthdata = stationdata.monthMap.get(stationmonth);

                    HashMap<WeekDay, double[][]> globalmonthdata = globalDemand.get(stationmonth);
                    if (globalmonthdata == null) {
                        globalmonthdata = new HashMap<WeekDay, double[][]>();
                        globalDemand.put(stationmonth, globalmonthdata);
                    }

                    for (WeekDay stationday : stationmonthdata.dayMap.keySet()) {
                        DayData stationdaydata = stationmonthdata.dayMap.get(stationday);

                        double[][] globaldaydata = globalmonthdata.get(stationday);
                        if (globaldaydata == null) {
                            globaldaydata = new double[24][2];
                            for (int i = 0; i < 14; i++) {
                                globaldaydata[i][0] = 0;
                                globaldaydata[i][1] = 0;
                            }
                            globalmonthdata.put(stationday, globaldaydata);
                        }

                        for (int i = 0; i < 24; i++) {
                            //                               if (i==0 && stationmonth==Month.Jan && stationday==Day.Mon){
                            //                                 System.out.println("add");
                            //                           } 
                            globaldaydata[i][0] = globaldaydata[i][0]
                                    + ((double) stationdaydata.data[i][0] / (double) stationdaydata.data[i][2]);
                            globaldaydata[i][1] = globaldaydata[i][1]
                                    + ((double) stationdaydata.data[i][1] / (double) stationdaydata.data[i][2]);
                        }
                    }
                }
            }
        }

        double getAverageStationDemand(Month month, WeekDay day, int hour, boolean take) {
            return (getDemandGlobal(month, day, hour, take) / stationMap.size());
        }

        //if take==true returns the take demand otherwise returns the return demand
        double getDemandGlobal(Month month, WeekDay day, int hour, boolean take) {
            HashMap<WeekDay, double[][]> globalmonthdata = globalDemand.get(month);
            if (globalmonthdata == null) {
                throw new RuntimeException("no global demand available for this period");
            }

            double[][] globaldaydata = globalmonthdata.get(day);
            if (globaldaydata == null) {
                throw new RuntimeException("no global demand available for this period");
            }

            if (take) {
                return globaldaydata[hour][0];
            } else {
                return globaldaydata[hour][1];
            }
        }

        //if take==true returns the take demand otherwise returns the return demand
        DemandResult getDemandStation(int station, Month month, WeekDay day, int hour, boolean take) {
            StationData sd = stationMap.get(station);
            if (sd == null) {
                return new DemandResult(false, Double.NaN);
            }
            return sd.getDemand(month, day, hour, take);
        }

        double getEntries(int station, Month month, WeekDay day, int hour) {
            StationData sd = stationMap.get(station);
            if (sd == null) {
                throw new RuntimeException("entries do not exist");
            }
            return sd.getEntries(month, day, hour);
        }

    }

    //class for storing the demand for one sinle station or also for the sum of all stations
    private static class StationData {

        HashMap<Month, MonthData> monthMap;

        StationData() {
            monthMap = new HashMap<Month, MonthData>(15);
        }

        private void addData(Month month, WeekDay day, int hour, int take, int ret) {
            MonthData data = monthMap.get(month);
            if (data == null) {
                data = new MonthData();
                monthMap.put(month, data);
            }
            data.add(day, hour, take, ret);
        }

        void add(Month month, WeekDay day, int hour, int take, int ret) {
            //add the data for the month
            addData(month, day, hour, take, ret);

            //add winter or summer 
            if (month == Month.Nov || month == Month.Dic || month == Month.Jan || month == Month.Feb || month == Month.Mar) {
                addData(Month.Winter, day, hour, take, ret);
            } else {
                addData(Month.Summer, day, hour, take, ret);
            }
            //add to allmonth
            addData(Month.All, day, hour, take, ret);
        }

        //if take==true returns the take demand otherwise returns the return demand
        DemandResult getDemand(Month month, WeekDay day, int hour, boolean take) {
            MonthData data = monthMap.get(month);
            if (data == null) {
                return new DemandResult(false, Double.NaN);
            }
            return data.getDemand(day, hour, take);
        }

        double getEntries(Month month, WeekDay day, int hour) {
            MonthData data = monthMap.get(month);
            if (data == null) {
                throw new RuntimeException("no demand data available: month " + month);
            }
            return data.getEntries(day, hour);
        }
    }

    private static class MonthData {

        HashMap<WeekDay, DayData> dayMap;

        MonthData() {
            dayMap = new HashMap<WeekDay, DayData>(9);
        }

        private void addData(WeekDay day, int hour, int take, int ret) {
            DayData data = dayMap.get(day);
            if (data == null) {
                data = new DayData();
                dayMap.put(day, data);
            }
            data.add(hour, take, ret);
        }

        void add(WeekDay day, int hour, int take, int ret) {
            //add to the day
            addData(day, hour, take, ret);
            //add weekend or weekday
            if (day == WeekDay.Sat || day == WeekDay.Sun) {
                addData(WeekDay.Weekend, hour, take, ret);
            } else {
                addData(WeekDay.Weekday, hour, take, ret);
            }
        }

        //if take==true returns the take demand otherwise returns the return demand
        DemandResult getDemand(WeekDay day, int hour, boolean take) {
            DayData data = dayMap.get(day);
            if (data == null) {
                return new DemandResult(false, Double.NaN);
            }
            return data.getDemand(hour, take);
        }

        double getEntries(WeekDay day, int hour) {
            DayData data = dayMap.get(day);
            if (data == null) {
                throw new RuntimeException("no demand data available: day");
            }
            return data.getEntries(hour);
        }
    }

    private static class DayData {

        int[][] data;

        DayData() {
            data = new int[24][3];
            for (int i = 0; i < 24; i++) {
                data[i][0] = 0;
                data[i][1] = 0;
                data[i][2] = 0;
            }
        }

        void add(int hour, int take, int ret) {
            data[hour][0] += take;
            data[hour][1] += ret;
            data[hour][2]++;
        }
        //if take==true returns the take demand otherwise returns the return demand

        DemandResult getDemand(int hour, boolean take) {
            if (take) {
                return new DemandResult(true, data[hour][0] / (double) data[hour][2]);
            } else {
                return new DemandResult(true, data[hour][1] / (double) data[hour][2]);
            }
        }

        double getEntries(int hour) {
            return data[hour][2];
        }
    }

    public static void main(String[] args) throws Exception {

        String projectDir = "/Users/holger/workspace/BikeProjects/Bike3S/Bike3S-Simulator";
        String demandDataPath = projectDir + "/../demandDataMadrid0817_0918.csv";
        FileBasedDemandManager demandManager = new FileBasedDemandManager(demandDataPath);

        String startdatetime="2017-10-05T09:00:00";
        LocalDateTime currentSimulationDateTime=LocalDateTime.parse(startdatetime);
        double x=demandManager.getStationReturnRateIntervall(62, currentSimulationDateTime, 3600);
        x=demandManager.getStationTakeRateIntervall(62, currentSimulationDateTime, 3600);
        x=demandManager.getGlobalTakeRateIntervall(currentSimulationDateTime, 3600);
        x=demandManager.getGlobalReturnRateIntervall(currentSimulationDateTime, 3600);
         
        startdatetime="2017-10-05T09:30:00";
        currentSimulationDateTime=LocalDateTime.parse(startdatetime);
        x=demandManager.getStationReturnRateIntervall(62, currentSimulationDateTime, 1800);
        x=demandManager.getStationTakeRateIntervall(62, currentSimulationDateTime, 1800);
        x=demandManager.getGlobalTakeRateIntervall(currentSimulationDateTime, 1800);
        x=demandManager.getGlobalReturnRateIntervall(currentSimulationDateTime, 1800);

        startdatetime="2017-10-05T09:50:10";
        currentSimulationDateTime=LocalDateTime.parse(startdatetime);
        x=demandManager.getStationReturnRateIntervall(62, currentSimulationDateTime, 3600);
        x=demandManager.getStationTakeRateIntervall(62, currentSimulationDateTime, 3600);
        x=demandManager.getGlobalTakeRateIntervall(currentSimulationDateTime, 3600);
        x=demandManager.getGlobalReturnRateIntervall(currentSimulationDateTime, 3600);
       
        Month[] m = Month.values();
        WeekDay[] d = WeekDay.values();
        System.out.println("!!!!!Station demand:");

        
        Month mm = Month.Oct;
        WeekDay dd = WeekDay.Thu;
        for (int i = 0; i < 24; i++) {
            for (Integer si : demandManager.dem.stationMap.keySet()) {
                double take = demandManager.getStationTakeRatePerHour(si, mm, dd, i);
                double ret = demandManager.getStationReturnRatePerHour(si, mm, dd, i);
                System.out.println(
                        "Station : " + si + " : demand Month: " + mm + " : day: " + dd + " : hour: " + i
                        + " : take: " + take
                        + " : return: " + ret
                        + " : entries: " + demandManager.dem.getEntries(si, mm, dd, i));

            }
        }

        /*      int stationsum = 0;
        for (Month mm : m) {
            for (Day dd : d) {
                for (int i = 0; i < 24; i++) {
                    for (Integer si : demandManager.dem.stationMap.keySet()) {
                        DemandResult take = demandManager.getTakeDemandStation(si, mm, dd, i);
                        DemandResult ret = demandManager.getReturnDemandStation(si, mm, dd, i);
                        if (!take.hasdemand || !ret.hasdemand) {
                            System.out.println(
                                    "Station : " + si + " : demand Month: " + mm + " : day: " + dd + " : hour: " + i
                                    + " : take: not avail."
                                    + " : return: not avail."
                                    + " : entries: not avail.");

                        } else {
                            System.out.println(
                                    "Station : " + si + " : demand Month: " + mm + " : day: " + dd + " : hour: " + i
                                    + " : take: " + take.demand
                                    + " : return: " + ret.demand
                                    + " : entries: " + demandManager.dem.getEntries(si, mm, dd, i));
                            stationsum++;
                        }
                    }
                }
            }
        }
        System.out.println("!!!!!Station demand:");
        for (Month mm : m) {
            for (Day dd : d) {
                for (int i = 0; i < 24; i++) {
                    DemandResult take = demandManager.getTakeDemandGlobal(mm, dd, i);
                    DemandResult ret = demandManager.getReturnDemandGlobal(mm, dd, i);
                    if (!take.hasdemand || !ret.hasdemand) {
                        System.out.println(
                                "Total demand :  : demand Month: " + mm + " : day: " + dd + " : hour: " + i
                                + " : take: not avail."
                                + " : return: not avail.");

                    } else {
                        //            if (mm!=Month.Jan && mm!=Month.Feb && mm!=Month.Winter && mm!=Month.All) continue;
                        System.out.println(
                                "Total demand :  : demand Month: " + mm + " : day: " + dd + " : hour: " + i
                                + " : take: " + take.demand
                                + " : return: " + ret.demand);
                    }
                }
            }

        }
         */
    }

}
