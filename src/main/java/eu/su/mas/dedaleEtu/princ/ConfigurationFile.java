package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.env.EnvironmentType;
import eu.su.mas.dedale.env.GeneratorType;

public final class ConfigurationFile {

  // Distributed or not, and is the current computer in charge of the
  // main-container
  public static boolean PLATFORMisDISTRIBUTED = false;

  public static boolean COMPUTERisMAIN = true;

  // network configuration
  public static String PLATFORM_HOSTNAME = "127.0.0.1";
  public static String PLATFORM_ID = "Ithaq";
  public static Integer PLATFORM_PORT = 8887;

  // List of containers to be created on the current computer
  public static String LOCAL_CONTAINER_NAME = PLATFORM_ID + "_" + "container1";
  public static String LOCAL_CONTAINER2_NAME = PLATFORM_ID + "_" + "container2";
  public static String LOCAL_CONTAINER3_NAME = PLATFORM_ID + "_" + "container3";
  public static String LOCAL_CONTAINER4_NAME = PLATFORM_ID + "_" + "container4";

  // The environment is either a GraphStream (2D discrete) or JME (3D continuous)
  // one.
  public static EnvironmentType ENVIRONMENT_TYPE = EnvironmentType.GS;

  // The environment is either manually designed, or generated with a specific
  // generator.
  // public static GeneratorType GENERATOR_TYPE = GeneratorType.MANUAL;
  public static GeneratorType GENERATOR_TYPE = GeneratorType.MANUAL;

  // The GateKeeper is in charge of the Platform and of the agents within, do not
  // change its name.
  public static String DEFAULT_GATEKEEPER_NAME = "GK";

  // Give the topology
  public static String INSTANCE_TOPOLOGY = "resources/topology/map2025-topologyExam";
  // public static String INSTANCE_TOPOLOGY = "resources/topology/TinyMap";

  // Give the elements available on the map, if any
  // If the environment is loaded but you do not want to define elements on the
  // map
  // public static String INSTANCE_CONFIGURATION_ELEMENTS =
  // "./resources/treasureHunt/map2019-elementsExam1.json";
  public static String INSTANCE_CONFIGURATION_ELEMENTS = "resources/treasureHunt/map2025-elements.json";

  // Size of the generated environment, mandatory
  public static Integer ENVIRONMENT_SIZE = 6;

  // Parameters required for some generators (see dedale.gitlab.io)
  public static Integer OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1 = 1;

  public static Integer[] GENERATOR_PARAMETERS = {
      ENVIRONMENT_SIZE,
      OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1
  };

  // Wumpus proximity detection radius
  public static final Integer DEFAULT_DETECTION_RADIUS = 1;

  // Agents communication radius
  public static Integer DEFAULT_COMMUNICATION_REACH = 3;

  // Elements on the map
  public static boolean ACTIVE_WELL = false;

  public static boolean ACTIVE_GOLD = true;
  public static boolean ACTIVE_DIAMOND = true;

  // Must'nt be null as it describes the native agents' capabilities
  public static String INSTANCE_CONFIGURATION_ENTITIES = "resources/map2025-entities.json";
}
