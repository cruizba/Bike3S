/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.history;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.history.entities.HistoricEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author holger
 */
public class HistoryJsonClasses {
    
    
        /**
     * It obtains the Json identifier string of a specific historical class.
     *
     * @param historicClass It is the history class whose identifier wants to be
     * found out.
     * @return the string of a Json identifier corresponding to the specified
     * histry class.
     */
    public static String getJsonIdentifier(Class<? extends HistoricEntity> historicClass) {
        JsonIdentifier[] jsonIdentifiers = historicClass.getAnnotationsByType(JsonIdentifier.class);

        if (jsonIdentifiers.length == 0) {
            throw new IllegalStateException("No annotation @JsonIdentifier found for " + historicClass);
        }

        if (jsonIdentifiers.length > 1) {
            throw new IllegalStateException("Found more than one @JsonIdentifier annotation in inheritance chain for " + historicClass);
        }

        return jsonIdentifiers[0].value();
    }

        /**
     * This class represents an event and contains the changes which have
     * occurred with respect to the previus event.
     *
     * @author IAgroup
     */
    public static class EventEntry {

        /**
         * It is the name of the event.
         */
        @Expose
        private String name;

        /**
         * It is the order of the event at the specified time (necessary if
         * there are more than one events at the same time).
         */
        @Expose
        private int order;
        
        @Expose
        private Event.EventResult result;
        
        @Expose
        private Collection<IdReference> involvedEntities;

        @Expose
        private Map<String, List<JsonObject>> changes;
        @Expose
        private Map<String, List<JsonObject>> newEntities;
        @Expose
        private Map<String, List<JsonObject>> oldEntities;

        EventEntry(String name, int order, Event.EventResult res, Collection<IdReference> involved, Map<String, List<JsonObject>> changes, 
                Map<String, List<JsonObject>> newEntities, Map<String, List<JsonObject>> oldEntities) {
            this.name = name;
            this.order = order;
            this.changes = changes;
            this.newEntities = newEntities;
            this.oldEntities = oldEntities;
            this.result=res;
            this.involvedEntities=involved;
        }

        public String getName() {
            return name;
        }

        public Event.EventResult getResult() {
            return result;
        }

        public Collection<IdReference> getInvolvedEntities() {
            return involvedEntities;
        }

        public Map<String, List<JsonObject>> getNewEntities() {
            return newEntities;
        }
        
    }

    /**
     * This class rpresents a time instant of the simulation and contains all
     * the events which happen at this moment.
     *
     * @author IAgroup
     *
     */
    public static class TimeEntry {

        /**
         * It is the moment when the events happen.
         */
        @Expose
        private int time;

        /**
         * They are the vents which happen at the specific moment. if the time
         * is the same the orden of the events is by increasing order
         */
        @Expose
        private List<EventEntry> events;

        TimeEntry(int time, List<EventEntry> events) {
            this.time = time;
            this.events = events;
        }

        public int getTime() {
            return time;
        }

        public List<EventEntry> getEvents() {
            return events;
        }
        
    }

    public static class IdReference {

        @Expose
        private String type;

        @Expose
        private Object id;

        private IdReference(Class<? extends HistoricEntity> type, Object id) {
            this.type = getJsonIdentifier(type);
            this.id = id;
        }

        public IdReference(Class<? extends HistoricEntity> type, Integer id) {
            this(type, (Object) id);
        }

        public IdReference(Class<? extends HistoricEntity> type, List<Integer> idList) {
            this(type, (Object) idList);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IdReference that = (IdReference) o;
            return Objects.equals(type, that.type)
                    && Objects.equals(id, that.id);
        }

        public String getType() {
            return type;
        }

        public Object getId() {
            return id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }
    }


}
