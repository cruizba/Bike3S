package es.urjc.ia.bikesurbanfleets.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphManager;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphManagerParameters;
import es.urjc.ia.bikesurbanfleets.common.graphs.GraphManagerType;
import es.urjc.ia.bikesurbanfleets.common.graphs.exceptions.GraphHopperIntegrationException;
import es.urjc.ia.bikesurbanfleets.common.util.MessageGuiFormatter;
import es.urjc.ia.bikesurbanfleets.comparators.StationComparator;
import es.urjc.ia.bikesurbanfleets.consultSystems.InformationSystem;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystem;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystemParameters;
import es.urjc.ia.bikesurbanfleets.consultSystems.RecommendationSystemType;
import es.urjc.ia.bikesurbanfleets.infraestructure.InfraestructureManager;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SimulationServices {

    private final String INIT_EXCEPTION_MESSAGE = "Simulation Service is not correctly started."
            + " You should init all the services";

    private InfraestructureManager infrastructureManager;
    private RecommendationSystem recommendationSystem;
    private InformationSystem informationSystem;
    private GraphManager graphManager;

    private Set<Class<?>> graphClasses;
    private Set<Class<?>> recommendationSystemClasses;

    private Gson gson = new Gson();

    public SimulationServices(SimulationServiceConfigData configData)
            throws IOException, GraphHopperIntegrationException {

        Reflections reflections = new Reflections();
        this.graphClasses = reflections.getTypesAnnotatedWith(GraphManagerType.class);
        this.recommendationSystemClasses = reflections.getTypesAnnotatedWith(RecommendationSystemType.class);

        this.infrastructureManager = new InfraestructureManager(configData.getStations(), configData.getBbox());
        this.graphManager = initGraphManager(configData.getGraphManagerType(), configData.getGraphParameters());
       this.informationSystem = new InformationSystem(this.infrastructureManager);
        this.recommendationSystem = initRecommendationSystem(configData.getRecomSystemType());
    }

    private RecommendationSystem initRecommendationSystem(JsonObject recsystemdef) throws IllegalStateException {

        //find the usertype
        String type = recsystemdef.get("typeName").getAsString();

        for (Class<?> recommendationSystemClass : recommendationSystemClasses) {
            String recomTypeAnnotation = recommendationSystemClass.getAnnotation(RecommendationSystemType.class).value();
            if (recomTypeAnnotation.equals(type)) {

                try {
                    Constructor constructor = recommendationSystemClass.getConstructor(JsonObject.class, InfraestructureManager.class);
                    RecommendationSystem recomSys = (RecommendationSystem) constructor.newInstance(recsystemdef, this.infrastructureManager);
                    return recomSys;
                } catch (Exception e) {
                    MessageGuiFormatter.showErrorsForGui("Error Creating Recommendation System");
                    MessageGuiFormatter.showErrorsForGui(e);
                }
            }
        }
        return null;
    }

    private GraphManager initGraphManager(String graphManagerType, JsonElement parameters)
            throws IllegalStateException {

        for (Class<?> graphClass : graphClasses) {
            String graphTypeAnnotation = graphClass.getAnnotation(GraphManagerType.class).value();
            if (graphTypeAnnotation.equals(graphManagerType)) {
                List<Class<?>> innerClasses = Arrays.asList(graphClass.getClasses());
                Class<?> graphParametersClass = null;

                for (Class<?> innerClass : innerClasses) {
                    if (innerClass.getAnnotation(GraphManagerParameters.class) != null) {
                        graphParametersClass = innerClass;
                        break;
                    }
                }

                try {
                    if (graphParametersClass != null) {
                        Constructor constructor = graphClass.getConstructor(graphParametersClass);
                        GraphManager graphManager = (GraphManager) constructor.newInstance(gson.fromJson(parameters, graphParametersClass));
                        return graphManager;
                    } else {
                        Constructor constructor = graphClass.getConstructor();
                        GraphManager graphManager = (GraphManager) constructor.newInstance();
                        return graphManager;
                    }
                } catch (Exception e) {
                    MessageGuiFormatter.showErrorsForGui("Error Creating Graph Manager");
                    MessageGuiFormatter.showErrorsForGui(e);
                }
            }
        }
        return null;
    }

    public InfraestructureManager getInfrastructureManager() throws IllegalStateException {
        checkService();
        return this.infrastructureManager;
    }

    public RecommendationSystem getRecommendationSystem() {
        checkService();
        return this.recommendationSystem;
    }

    public InformationSystem getInformationSystem() {
        checkService();
        return this.informationSystem;
    }

    public GraphManager getGraphManager() {
        checkService();
        return graphManager;
    }

     private void checkService() throws IllegalStateException {
        if (recommendationSystem == null || graphManager == null) {
            throw new IllegalStateException(INIT_EXCEPTION_MESSAGE);
        }
    }

}
