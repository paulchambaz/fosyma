package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agents.GateKeeperAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.DummyTankerAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;

// Principal manages the initialization and execution of the multi-agent
// system. Handles platform creation, container management, agent
// initialization, and system startup for the Dedale environment.
public class Principal {

  private static HashMap<String, ContainerController> containerList = new HashMap<>();
  private static List<AgentController> agentList;
  private static Runtime runtime;

  // Main entry point that initializes the JADE platform and starts agents.
  // Creates either a main platform or connects to an existing one based on
  // configuration.
  public static void main(String[] args) {
    if (ConfigurationFile.COMPUTERisMAIN) {
      runtime = createEmptyPlatform(containerList);
    } else {
      containerList.putAll(createAndConnectContainer(
          ConfigurationFile.LOCAL_CONTAINER_NAME,
          ConfigurationFile.PLATFORM_HOSTNAME,
          ConfigurationFile.PLATFORM_ID,
          ConfigurationFile.PLATFORM_PORT));
    }

    agentList = createAgents(containerList);
    startAgents(agentList);
  }

  // createEmptyPlatform creates an empty JADE platform with main container and
  // monitoring agents. Sets up the platform with the specified hostname, port
  // and ID from configuration.
  private static Runtime createEmptyPlatform(HashMap<String, ContainerController> containerList) {
    Runtime runtime = Runtime.instance();

    // create a platform (main container + DF + AMS)
    Profile mainProfile = new ProfileImpl(
        ConfigurationFile.PLATFORM_HOSTNAME,
        ConfigurationFile.PLATFORM_PORT,
        ConfigurationFile.PLATFORM_ID);
    System.out.println("Launching a main-container..." + mainProfile);
    AgentContainer mainContainerRef = runtime.createMainContainer(mainProfile);

    // create the containers
    containerList.putAll(createContainers(runtime));

    // create monitoring agents: rma agent and sniffer agent
    createMonitoringAgents(mainContainerRef);

    System.out.println("Plaform ok");
    return runtime;
  }

  // createContainers creates standard containers for agent deployment.
  // Generates containers based on names defined in the configuration file.
  private static HashMap<String, ContainerController> createContainers(Runtime runtime) {
    System.out.println("Launching containers ...");
    HashMap<String, ContainerController> containerList = new HashMap<>();

    // Create the standard set of containers
    String[] containerNames = {
        ConfigurationFile.LOCAL_CONTAINER_NAME,
        ConfigurationFile.LOCAL_CONTAINER2_NAME,
        ConfigurationFile.LOCAL_CONTAINER3_NAME,
        ConfigurationFile.LOCAL_CONTAINER4_NAME,
    };

    for (String containerName : containerNames) {
      ProfileImpl containerProfile = new ProfileImpl(
          ConfigurationFile.PLATFORM_HOSTNAME,
          ConfigurationFile.PLATFORM_PORT,
          ConfigurationFile.PLATFORM_ID);

      containerProfile.setParameter(Profile.CONTAINER_NAME, containerName);
      System.out.println("Launching container " + containerProfile);
      ContainerController containerRef = runtime.createAgentContainer(containerProfile);
      containerList.put(containerName, containerRef);
    }

    System.out.println("Launching containers done");
    return containerList;
  }

  // createAndConnectContainer creates and connects a single container to an
  // existing platform. Used for distributed deployment across multiple
  // machines.
  private static HashMap<String, ContainerController> createAndConnectContainer(
      String containerName, String host, String platformID, Integer port) {
    HashMap<String, ContainerController> containerList = new HashMap<>();
    Runtime runtime = Runtime.instance();

    if (port == null) {
      port = ConfigurationFile.PLATFORM_PORT;
    }

    System.out.println(
        "Create and Connect container: " + containerName
            + ", host : " + host
            + ", platformID: " + platformID
            + ", port: " + port);

    ProfileImpl containerProfile = new ProfileImpl(host, port, platformID);
    containerProfile.setParameter(Profile.CONTAINER_NAME, containerName);
    ContainerController containerRef = runtime.createAgentContainer(containerProfile);

    containerList.put(containerName, containerRef);
    return containerList;
  }

  // createMonitoringAgents creates monitoring agents (RMA and Sniffer) on the
  // main container. These agents provide debugging and visualization
  // capabilities.
  private static void createMonitoringAgents(ContainerController mainContainer) {
    Assert.assertNotNull(mainContainer);

    try {
      // create rma agent
      System.out.println("Launching the rma agent on the main container...");
      AgentController rmaAgent = mainContainer.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
      rmaAgent.start();

      // create sniffer agent
      System.out.println("Launching sniffer agent on the main container...");
      AgentController snifferAgent = mainContainer.createNewAgent("sniffeur", "jade.tools.sniffer.Sniffer",
          new Object[0]);
      snifferAgent.start();

    } catch (StaleProxyException e) {
      e.printStackTrace();
      System.out.println("Launching of monitoring agents failed");
    }
  }

  // createAgents creates all system agents including the GateKeeper and
  // explorer agents. Initializes agents with appropriate parameters based on
  // configuration.
  private static List<AgentController> createAgents(
      HashMap<String, ContainerController> containerList) {
    System.out.println("Launching agents...");
    List<AgentController> agentList = new ArrayList<>();

    if (ConfigurationFile.COMPUTERisMAIN) {
      createGateKeeperAgent(containerList, agentList);
    }

    ContainerController agentContainer = containerList.get(ConfigurationFile.LOCAL_CONTAINER2_NAME);
    Assert.assertNotNull("This container does not exist", agentContainer);

    List<String> agents = Arrays.asList("dante", "virgilio", "beatrice", "lucia", "bernardo");

    for (String agent : agents) {
      List<String> otherAgents = new ArrayList<>(agents);
      otherAgents.remove(agent);
      Object[] otherAgentsArray = otherAgents.toArray();
      createExploreAgent(agentContainer, agent, otherAgentsArray, agentList);
    }

    createSiloAgent(agentContainer, agentList);
    // createGolemAgent(agentContainer, agentList);

    System.out.println("Agents created...");
    return agentList;
  }

  private static void createGateKeeperAgent(
      HashMap<String, ContainerController> containerList,
      List<AgentController> agentList) {
    ContainerController mainContainer = containerList.get(ConfigurationFile.LOCAL_CONTAINER_NAME);
    Assert.assertNotNull("This container does not exist", mainContainer);

    try {
      Object[] gateKeeperParams = new Object[] {
          ConfigurationFile.ENVIRONMENT_TYPE,
          ConfigurationFile.GENERATOR_TYPE,
          ConfigurationFile.INSTANCE_TOPOLOGY,
          ConfigurationFile.INSTANCE_CONFIGURATION_ELEMENTS,
          ConfigurationFile.ACTIVE_DIAMOND,
          ConfigurationFile.ACTIVE_GOLD,
          ConfigurationFile.ACTIVE_WELL,
          ConfigurationFile.GENERATOR_PARAMETERS
      };

      AgentController gateKeeperAgent = mainContainer.createNewAgent(
          ConfigurationFile.DEFAULT_GATEKEEPER_NAME,
          GateKeeperAgent.class.getName(),
          gateKeeperParams);

      agentList.add(gateKeeperAgent);
      System.out.println(ConfigurationFile.DEFAULT_GATEKEEPER_NAME + " launched");
    } catch (StaleProxyException e) {
      e.printStackTrace();
    }
  }

  // createExploreAgent creates an explorer agent with the specified name and
  // parameters. Adds the created agents to the provided agent list.
  private static void createExploreAgent(
      ContainerController container, String agentName, Object[] parameters,
      List<AgentController> agentList) {
    try {
      AgentController agent = createNewDedaleAgent(
          container,
          agentName,
          ExploreCoopAgent.class.getName(),
          parameters);
      agentList.add(agent);
      System.out.println("Agent " + agentName + " was created");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Failed to create agent: " + agentName);
    }
  }

  private static void createSiloAgent(
      ContainerController container,
      List<AgentController> agentList) {
    try {
      AgentController siloAgent = createNewDedaleAgent(
          container,
          "Silo",
          DummyTankerAgent.class.getName(),
          new Object[] {});
      agentList.add(siloAgent);
      System.out.println("Silo agent was created");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Failed to create agent: Tank");
    }

  }

  private static void createGolemAgent(
      ContainerController container,
      List<AgentController> agentList) {
    try {
      AgentController golemAgent = createNewDedaleAgent(
          container,
          "Golem",
          // TODO: replace with proper class: DummyWumpus.class.getName(),
          "eu.su.mas.dedaleEtu.mas.agents.dummies.wumpus.DummyWumpus",
          new Object[] {});
      agentList.add(golemAgent);
      System.out.println("Golem agent was created");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Failed to create agent: Golem");
    }
  }

  // startAgents starts all agents in the provided list. Handles potential
  // exceptions during agent startup.
  private static void startAgents(List<AgentController> agentList) {
    System.out.println("Starting agents...");

    for (final AgentController ac : agentList) {
      try {
        ac.start();
      } catch (StaleProxyException e) {
        e.printStackTrace();
      }
    }

    System.out.println("Agents started...");
  }

  // createNewDedaleAgent creates a new Dedale agent with entity parameters and
  // additional parameters. Loads entity characteristics from configuration and
  // merges with custom parameters.
  private static AgentController createNewDedaleAgent(
      ContainerController container,
      String agentName,
      String className,
      Object[] additionalParameters) {
    Object[] entityParameters = AbstractDedaleAgent.loadEntityCaracteristics(
        agentName,
        ConfigurationFile.INSTANCE_CONFIGURATION_ENTITIES);

    Object[] allParameters = mergeArrays(entityParameters, additionalParameters);

    AgentController agent = null;
    try {
      agent = container.createNewAgent(agentName, className, allParameters);
    } catch (StaleProxyException e) {
      e.printStackTrace();
    }

    Assert.assertNotNull(agent);
    System.out.println(agentName + " launched");
    return agent;
  }

  // mergeArrays merges two object arrays into a single array. Used to combine
  // entity parameters with additional parameters for agent creation.
  private static Object[] mergeArrays(Object[] array1, Object[] array2) {
    Assert.assertNotNull(array1);

    if (array2 == null) {
      return array1;
    }

    Object[] result = new Object[array1.length + array2.length];

    System.arraycopy(array1, 0, result, 0, array1.length);
    System.arraycopy(array2, 0, result, array1.length, array2.length);

    return result;
  }
}
