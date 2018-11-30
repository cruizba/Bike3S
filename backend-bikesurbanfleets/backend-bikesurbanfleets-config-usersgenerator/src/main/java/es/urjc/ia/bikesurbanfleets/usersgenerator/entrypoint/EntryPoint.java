package es.urjc.ia.bikesurbanfleets.usersgenerator.entrypoint;

import es.urjc.ia.bikesurbanfleets.usersgenerator.SingleUser;

import java.util.List;

/**
 * It is an event generator for user appearances.
 * It represents an entry point at system geographic map where a unique user or several users
 * appear and start interacting with the system.
 * @author IAgroup
 *
 */
public abstract class EntryPoint {

    protected String entryPointType;

    public static int TOTAL_SIMULATION_TIME;
    /**
     * It generate single users for the configuration file,
     * which are the main events that starts the simulation execution.
     * @return a list of single users
     */

    public String getEntryPointType() {
        return entryPointType;
    }

    public abstract List<SingleUser> generateUsers();

}