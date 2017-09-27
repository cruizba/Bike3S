package com.urjc.iagroup.bikesurbanfloats.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.urjc.iagroup.bikesurbanfloats.entities.Station;

public class ConfigJacksonReader {
	
	public final static String JSON_ATR_STATION = "stations";
	public final static String JSON_ATR_ENTRYPOINTS = "entryPoints";
	public final static String JSON_ATR_TIME_RESERVE = "reservationTime";
	public final static String JSON_ATR_TIME_SIMULATION = "totalTimeSimulation";
	
<<<<<<< Updated upstream:backend-bikesurbanfloats/src/main/java/com/urjc/iagroup/bikesurbanfloats/config/ConfigJacksonReader.java
	public ConfigJacksonReader(String configFile) {
		this.configFile = configFile;
	}

	public ConfigInfo getConfigInfo() {
		return configInfo;
	}

	public void setConfigInfo(ConfigInfo configInfo) {
		this.configInfo = configInfo;
=======
	private String stationsFileName;
	private String entryPointsFileName;
	private String configSimulationFileName;
	
	public ConfigJsonReader(String stationsFileName, String entryPointsFileName, String configSimulationFileName) {
		this.stationsFileName = stationsFileName;
		this.entryPointsFileName = entryPointsFileName;
		this.configSimulationFileName = configSimulationFileName;
		
>>>>>>> Stashed changes:backend-bikesurbanfloats/src/main/java/com/urjc/iagroup/bikesurbanfloats/config/ConfigJsonReader.java
	}
	
	public void readJson() throws FileNotFoundException {
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Station.class, new StationDeserializer());
		Gson gson = gsonBuilder.create();
		
		//Stations
		FileInputStream inputStreamJson = new FileInputStream(new File(stationsFileName));
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStreamJson));
		ConfigInfo.stations = readStations(gson, bufferedReader);
		
		//EntryPoints
		inputStreamJson = new FileInputStream(new File(entryPointsFileName));
		bufferedReader = new BufferedReader(new InputStreamReader(inputStreamJson));
		ConfigInfo.entryPoints = readEntryPoints(gson, bufferedReader);
		
		//Configuration
		inputStreamJson = new FileInputStream(new File(configSimulationFileName));
		bufferedReader = new BufferedReader(new InputStreamReader(inputStreamJson));
		JsonObject jsonConfig = gson.fromJson(bufferedReader, JsonObject.class);
		ConfigInfo.reservationTime = jsonConfig.get(JSON_ATR_TIME_RESERVE).getAsInt();
		ConfigInfo.totalTimeSimulation = jsonConfig.get(JSON_ATR_TIME_SIMULATION).getAsInt();
	}
	
	private ArrayList<Station> readStations(Gson gson, BufferedReader bufferedReader) {
		ArrayList<Station> allStations = new ArrayList<>();
		JsonArray jsonStationsArray = gson.fromJson(bufferedReader, JsonObject.class)
				.get(JSON_ATR_STATION).getAsJsonArray();
		for(JsonElement elemStation: jsonStationsArray) {
			allStations.add(gson.fromJson(elemStation, Station.class));
		}
		return allStations;
	}
	
	private ArrayList<EntryPoint> readEntryPoints(Gson gson, BufferedReader bufferedReader) {
		ArrayList<EntryPoint> allEntryPoints = new ArrayList<>();
		JsonArray jsonStationsArray = gson.fromJson(bufferedReader, JsonObject.class)
				.get(JSON_ATR_ENTRYPOINTS).getAsJsonArray();
		for(JsonElement elemStation: jsonStationsArray) {
			allEntryPoints.add(gson.fromJson(elemStation, EntryPoint.class));
		}
		return allEntryPoints;
	}
	

}
