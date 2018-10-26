package es.urjc.ia.bikesurbanfleets.core;

import es.urjc.ia.bikesurbanfleets.common.util.JsonValidation;
import es.urjc.ia.bikesurbanfleets.common.util.JsonValidation.ValidationParams;
import es.urjc.ia.bikesurbanfleets.common.util.MessageGuiFormatter;
import es.urjc.ia.bikesurbanfleets.common.config.GlobalInfo;
import es.urjc.ia.bikesurbanfleets.core.config.*;
import es.urjc.ia.bikesurbanfleets.core.core.SimulationEngine;
import es.urjc.ia.bikesurbanfleets.core.exceptions.ValidationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
 



public class ApplicationHolger {

    //Program parameters
    private static String globalSchema;
    private static String usersSchema;
    private static String stationsSchema;
    private static String globalConfig;
    private static String usersConfig;
    private static String stationsConfig;
    private static String mapPath;
    private static String historyOutputPath;
    private static String validator;
    private static boolean callFromFrontend;

    
    private static CommandLine commandParser(String[] args) throws ParseException {
        
        Options options = new Options();
        options.addOption("globalSchema", true, "Directory to global schema validation");
        options.addOption("usersSchema", true, "Directory to users schema validation");
        options.addOption("stationsSchema", true, "Directory to stations schema validation");
        options.addOption("globalConfig", true, "Directory to the global configuration file");
        options.addOption("usersConfig", true, "Directory to the users configuration file");
        options.addOption("stationsConfig", true, "Directory to the stations configuration file");
        options.addOption("mapPath", true, "Directory to map");
        options.addOption("historyOutput", true, "History Path for the simulation");
        options.addOption("validator", true, "Directory to the js validator");
        options.addOption("callFromFrontend", false, "Backend has been called by frontend");
    
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
        
    }
    
    public static void main(String[] args) throws Exception {

        String test="paperAT2018/obedient_Holger_recomender_prueba";
        String basedir="/Users/holger/workspace/BikeProjects/Bike3S/Bike3STests/";
        GlobalInfo.DEBUG_DIR=basedir+test+ "/debug";
        System.out.println("Test:"+test);

        //Create auxiliary folders
        File auxiliaryDir = new File(GlobalInfo.TEMP_DIR);
        if(!auxiliaryDir.exists()) {
            auxiliaryDir.mkdirs();
        }
        auxiliaryDir = new File(GlobalInfo.DEBUG_DIR);
        if(!auxiliaryDir.exists()) {
            auxiliaryDir.mkdirs();
        }
       globalSchema = "";
        usersSchema = "";
        stationsSchema = "";
        globalConfig = basedir+ test +"/conf/global_configuration.json";
        usersConfig = basedir+ test +"/conf/users_configuration.json";
        stationsConfig = basedir+ test +"/conf/stations_configuration.json";
        mapPath = basedir+ "madrid.osm";
        historyOutputPath = basedir+ test +"/hist";
        validator = "";
        callFromFrontend = true;

  /*      CommandLine cmd;
        try {
            cmd = commandParser(args);
        } catch (ParseException e1) {
            System.out.println("Error reading params");
            throw e1;
        }

        globalSchema = cmd.getOptionValue("globalSchema");
        usersSchema = cmd.getOptionValue("usersSchema");
        stationsSchema = cmd.getOptionValue("stationsSchema");
        globalConfig = cmd.getOptionValue("globalConfig");
        usersConfig = cmd.getOptionValue("usersConfig");
        stationsConfig = cmd.getOptionValue("stationsConfig");
        mapPath = cmd.getOptionValue("mapPath");
        historyOutputPath = cmd.getOptionValue("historyOutput");
        validator = cmd.getOptionValue("validator");
        callFromFrontend = cmd.hasOption("callFromFrontend");
*/
       
   //     checkParams(); // If not valid, throws exception
        ConfigJsonReader jsonReader = new ConfigJsonReader(globalConfig, stationsConfig, usersConfig);

        try {
            GlobalInfo globalInfo = jsonReader.readGlobalConfiguration();
            UsersConfig usersInfo = jsonReader.readUsersConfiguration();
            StationsConfig stationsInfo = jsonReader.readStationsConfiguration();
            System.out.println("DEBUG MODE: " + globalInfo.isDebugMode());
            if(historyOutputPath != null) {
                globalInfo.setHistoryOutputPath(historyOutputPath);
            }

            //TODO mapPath not obligatory for other graph managers
            if(mapPath != null) {
                SimulationEngine simulation = new SimulationEngine(globalInfo, stationsInfo, usersInfo, mapPath);
                simulation.run();
            }
            else {
                MessageGuiFormatter.showErrorsForGui("You should specify a map directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void checkParams() throws Exception {

        String exMessage = null; // Message for exceptions
        String warningMessage = null;
        if(hasAllSchemasAndConfig()) {

            ValidationParams vParams = new ValidationParams();
            vParams.setSchemaDir(globalSchema).setJsonDir(globalConfig).setJsValidatorDir(validator);
            String globalConfigValidation = validate(vParams);

            vParams.setSchemaDir(usersSchema).setJsonDir(usersConfig);
            String usersConfigValidation = validate(vParams);

            vParams.setSchemaDir(stationsSchema).setJsonDir(stationsConfig);
            String stationsConfigValidation = validate(vParams);

            System.out.println(globalConfigValidation);
            System.out.println(usersConfigValidation);
            System.out.println(stationsConfigValidation);

            if((!globalConfigValidation.equals("OK")
                    || !usersConfigValidation.equals("OK") || !stationsConfigValidation.equals("OK"))) {

                exMessage = "JSON has errors \n Global configuration errors \n" + globalConfigValidation + "\n" +
                        "Stations configuration errors \n" + stationsConfigValidation + "\n" +
                        "Users configuration errors \n" + usersConfigValidation;

            } else if (globalConfigValidation.equals("NODE_NOT_INSALLED")) {

                exMessage = "Node is necessary to execute validator: " + validator + ". \n" +
                        "Verify if node is installed or install node";

            } else if(globalConfigValidation.equals("OK") && stationsConfigValidation.equals("OK")
                    && usersConfigValidation.equals("OK")) {

                System.out.println("Validation configuration input: OK\n");
            }
        }
        else if(globalConfig == null || stationsConfig == null || usersConfig == null) {
            exMessage = "You should specify a configuration file";
        }
        else if((globalSchema == null || usersSchema == null || stationsSchema == null) && validator != null) {
            exMessage = "You should specify all schema paths";

        }
        else if(validator == null && !callFromFrontend) {
            warningMessage = "Warning: you don't specify a validator, configuration file will not be validated on backend";
        }
        else if(mapPath == null) {
            exMessage = "You should specify a map directory";
        }

        if(exMessage != null) {
            System.out.println("Exception");
            throw new ValidationException(exMessage);
        }

        if(warningMessage != null) {
            System.out.println(warningMessage);
        }
    }

    private static boolean hasAllSchemasAndConfig() {
        return globalSchema != null && usersSchema != null && stationsSchema != null && globalConfig != null
                && stationsConfig != null && validator != null;
    }

    private static String validate(ValidationParams vParams) throws Exception {
        return JsonValidation.validate(vParams);
    }

}
