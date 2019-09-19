/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.urjc.ia.bikesurbanfleets.core.ManagingEvents;

import es.urjc.ia.bikesurbanfleets.common.interfaces.Entity;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.common.log.Debug;
import es.urjc.ia.bikesurbanfleets.common.util.MessageGuiFormatter;
import es.urjc.ia.bikesurbanfleets.services.fleetManager.FleetManager;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author holger
 */
public abstract class EventManaging implements Event {
    
   /**
     * It is the time instant when event happens.
     */
    protected int instant;
    protected List<Entity> newEntities;
    protected List<Entity> oldEntities;
    protected List<Entity> involvedEntities;
    private Event.RESULT_TYPE result;
    /**
     * It is the manager how issued the event.
     */
    protected FleetManager manager;

    public EventManaging(int instant, FleetManager m) {
        this.instant = instant;
        manager=m;
    }

    public int getInstant() {
        return instant;
    }

    public final String toString() {
        return print();
    }
    
    public List<Entity> getNewEntities() {
        return newEntities;
    }

    public List<Entity> getOldEntities() {
        return oldEntities;
    }

    public List<Entity> getInvolvedEntities() {
        return involvedEntities;
    }
    
    public Event.RESULT_TYPE getResult() {
        return result;
    };
    public void setResult(Event.RESULT_TYPE result){
        this.result=result;
    };


    /**
     * It proccesses the event so that the relevant changes at the system occur.
     */
    public abstract Event execute() throws Exception;


    /*
        =================
        DEBUG METHODS
        =================
     */
    /**
     * If debug mode is activated in the configuration file, logs will be
     * activated to debug users
     *
     * @throws IOException
     */
    public void debugEventLog(String message) {
        try {
            Debug.log(message, manager, this);
        } catch (IOException e) {
            MessageGuiFormatter.showErrorsForGui(e);
        }
    }

    /**
     * If debug mode is activated in the configuration file, logs will be
     * activated to debug users
     *
     * @throws IOException
     */
    public void debugEventLog() {
        try {
            Debug.log(manager, this);
        } catch (IOException e) {
            MessageGuiFormatter.showErrorsForGui(e);
        }
    }

    public void debugClose() {
        try {
            Debug.closeLog(manager, manager.getId());
        } catch (IOException e) {
            MessageGuiFormatter.showErrorsForGui(e);
        }
    }

    /*
        =================
        END - DEBUG METHODS
        =================

    /**
}
