package es.urjc.ia.bikesurbanfleets.core.config;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;

import es.urjc.ia.bikesurbanfleets.worldentities.stations.deserializers.StationDeserializer;

import java.util.List;

public class StationsConfig {

    /**
     * They are all the stations of the system obtained from the configuration file.
     */
    private List<JsonObject> stations;

    public List<JsonObject> getStations() { return stations; }

}
