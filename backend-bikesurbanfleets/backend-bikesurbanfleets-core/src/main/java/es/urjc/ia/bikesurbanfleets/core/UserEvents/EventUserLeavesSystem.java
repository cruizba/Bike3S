package es.urjc.ia.bikesurbanfleets.core.UserEvents;

import java.util.ArrayList;
import java.util.Arrays;
import es.urjc.ia.bikesurbanfleets.common.interfaces.Event;
import es.urjc.ia.bikesurbanfleets.worldentities.users.User;

public class EventUserLeavesSystem extends EventUser {

    EventUser.EXIT_REASON reason;
    
    public EventUserLeavesSystem(int instant, User user, EventUser.EXIT_REASON reason) {
        super(instant, user);
        this.involvedEntities = new ArrayList<>(Arrays.asList(user));
        this.newEntities = null;
        this.oldEntities=new ArrayList<>(Arrays.asList(user));
        this.reason=reason;
    }

    @Override
    public EventUser execute() throws Exception {
        debugEventLog("At enter the event");
        user.leaveSystem();
        user.setState(User.STATE.LEFT_SYSTEM);
        debugEventLog("User left the system");
        debugClose();
       
        //set the result of the event
        //the result of EventUserLeavesSystem is any of the possible exit rerasons
        setResultInfo(Event.RESULT_TYPE.SUCCESS, Event.ADDITIONAL_INFO.valueOf(this.reason.name()));

        //decide what to do afterwards
        return null;
    }
}
