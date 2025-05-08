package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import java.io.Serializable;
import jade.core.Agent;
import eu.su.mas.dedale.env.Location;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import eu.su.mas.dedaleEtu.princ.Utils;

public class Brain implements Serializable {
  private static final long serialVersionUID = -1333959882640838272L;

  public final String name;
  public final AgentMind mind;
  public final WorldMap map;
  public final EntityTracker entities;

  private BrainVisualization visualization;

  public Brain(String name) {
    this.name = name;
    this.mind = new AgentMind(this);
    this.map = new WorldMap(this);
    this.entities = new EntityTracker(this);
  }

  public synchronized boolean moveTo(Agent agent, String node) {
    Utils.waitFor(agent, 500);
    try {
      return ((AbstractDedaleAgent) agent).moveTo(new GsLocation(node));
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized String findClosestOpenNode(boolean excludeOccupied) {
    String position = this.entities.getPosition();
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    return map.findClosestOpenNode(position, (excludeOccupied) ? occupiedPositions : new ArrayList<>());
  }

  public synchronized String findClosestNode(List<String> nodes, boolean excludeOccupied) {
    String position = this.entities.getPosition();
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    return map.findClosestNode(position, nodes, (excludeOccupied) ? occupiedPositions : new ArrayList<>());
  }

  public synchronized void computePathToTarget(boolean excludeOccupied) {
    String position = this.entities.getPosition();
    String target = this.mind.getTargetNode();
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    List<String> path = map.findShortestPath(position, target,
        (excludeOccupied) ? occupiedPositions : new ArrayList<>());
    mind.setPathToTarget(path);
  }

  public synchronized String findRandomNode() {
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    return map.findRandomNode(occupiedPositions);
  }

  public synchronized void observe(Agent agent) {
    String position = ((AbstractDedaleAgent) agent).getCurrentPosition().getLocationId();
    this.entities.updatePosition(position);

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) agent).observe();

    updateTopology(position, observations);
    Map<String, String> observedAgents = processObservations(observations);
    detectForgottenEntities(observedAgents);

  }

  private void updateTopology(String position, List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    map.addNode(position, MapAttribute.CLOSED);
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      String accessibleNode = entry.getLeft().getLocationId();
      map.addNewNode(accessibleNode);
      if (!position.equals(accessibleNode)) {
        map.addEdge(position, accessibleNode);
      }
    }
  }

  private Map<String, String> processObservations(
      List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    Map<String, String> observedAgents = new HashMap<>();
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      String accessibleNode = entry.getLeft().getLocationId();
      for (Couple<Observation, String> observation : entry.getRight()) {
        Observation observeKind = observation.getLeft();
        String observed = observation.getRight();

        switch (observeKind) {
          case AGENTNAME:
            processAgentObservation(observed, accessibleNode, observedAgents);
            break;

          case GOLD:
          case DIAMOND:
            processTreasureObservation(accessibleNode, observeKind, observed);
            break;

          case LOCKSTATUS:
            processTreasureLockStatus(accessibleNode, observed);
            break;

          case LOCKPICKING:
            processTreasureLockpicking(accessibleNode, observed);
            break;

          case STRENGH:
            processTreasureStrength(accessibleNode, observed);
            break;

          default:
            break;
        }
      }
    }

    return observedAgents;
  }

  private void processAgentObservation(String agentName, String position, Map<String, String> observedAgents) {
    observedAgents.put(agentName, position);

    if (agentName.startsWith("Silo")) {
      entities.setSiloPosition(agentName, position);
    } else if (agentName.startsWith("Golem")) {
      entities.setGolemPosition(agentName, position);
    } else {
      entities.updateAgentPosition(agentName, position);
    }
  }

  private void processTreasureObservation(String nodeId, Observation type, String value) {
    int treasureValue = Integer.parseInt(value);
    entities.updateTreasure(nodeId, type, treasureValue, true, -1, -1);
  }

  private void processTreasureLockStatus(String nodeId, String value) {
    boolean treasureStatus = !Boolean.parseBoolean(value);
    entities.updateTreasureStatus(nodeId, treasureStatus);
  }

  private void processTreasureLockpicking(String nodeId, String value) {
    int treasureLockpicking = Integer.parseInt(value);
    entities.updateTreasureLockpinging(nodeId, treasureLockpicking);
  }

  private void processTreasureStrength(String nodeId, String value) {
    int treasureStrength = Integer.parseInt(value);
    entities.updateTreasureStrength(nodeId, treasureStrength);
  }

  private void detectForgottenEntities(Map<String, String> observedAgents) {

    Set<String> neighborhood = map.getNeighborhood(entities.getPosition());

    Map<String, String> observedNeighborhood = new HashMap<>();
    for (Map.Entry<String, String> entry : observedAgents.entrySet()) {
      String agentName = entry.getKey();
      String position = entry.getValue();
      if (neighborhood.contains(position)) {
        observedNeighborhood.put(position, agentName);
      }
    }

    Map<String, String> expectedNeighborhood = new HashMap<>();
    for (Map.Entry<String, AgentData> agent : entities.getAgents().entrySet()) {
      String agentName = agent.getKey();
      AgentData agentData = agent.getValue();
      String position = agentData.getPosition();

      if (position != null && neighborhood.contains(position)) {
        expectedNeighborhood.put(position, agentName);
      }
    }

    for (Map.Entry<String, SiloData> silo : entities.getSilos().entrySet()) {
      String siloName = silo.getKey();
      SiloData siloData = silo.getValue();
      String position = siloData.getPosition();

      if (position != null && neighborhood.contains(position)) {
        expectedNeighborhood.put(position, siloName);
      }
    }

    for (Map.Entry<String, GolemData> golem : entities.getGolems().entrySet()) {
      String golemName = golem.getKey();
      GolemData golemData = golem.getValue();
      String position = golemData.getPosition();

      if (position != null && neighborhood.contains(position)) {
        expectedNeighborhood.put(position, golemName);
      }
    }

    for (Map.Entry<String, String> entry : expectedNeighborhood.entrySet()) {
      String position = entry.getKey();
      String expectedAgent = entry.getValue();

      if (!observedNeighborhood.containsKey(position) ||
          !observedNeighborhood.get(position).equals(expectedAgent)) {

        if (expectedAgent.startsWith("Silo")) {
          SiloData siloData = entities.getSilos().get(expectedAgent);
          if (siloData != null && siloData.getUpdateCounter() > 10) {
            siloData.setPosition(null);
          }
        } else if (expectedAgent.startsWith("Golem")) {
          GolemData golemData = entities.getGolems().get(expectedAgent);
          if (golemData != null && golemData.getUpdateCounter() > 10) {
            golemData.setPosition(null);
          }
        } else {
          AgentData agentData = entities.getAgents().get(expectedAgent);
          if (agentData != null && agentData.getUpdateCounter() > 10) {
            entities.loseAgentPosition(expectedAgent);
          }
        }
      }
    }
  }

  public synchronized void selfLearn(Agent agent) {
    Observation treasureType = ((AbstractDedaleAgent) agent).getMyTreasureType();
    entities.setTreasureType(treasureType);

    Set<Couple<Observation, Integer>> expertise = ((AbstractDedaleAgent) agent).getMyExpertise();
    for (Couple<Observation, Integer> observation : expertise) {
      Observation observeKind = observation.getLeft();
      Integer observed = observation.getRight();
      entities.setExpertise(observeKind, observed);

    }

    List<Couple<Observation, Integer>> observationsFreespace = ((AbstractDedaleAgent) agent).getBackPackFreeSpace();
    for (Couple<Observation, Integer> freespace : observationsFreespace) {
      switch (freespace.getLeft()) {
        case GOLD:
          entities.setGoldCapacity(freespace.getRight());
          break;
        case DIAMOND:
          entities.setDiamondCapacity(freespace.getRight());
          break;
        default:
          break;
      }
    }
  }

  public synchronized void updateBackpack(Agent agent) {
    List<Couple<Observation, Integer>> observationsFreespace = ((AbstractDedaleAgent) agent).getBackPackFreeSpace();
    AgentData myself = entities.getMyself();
    int amount = 0;
    for (Couple<Observation, Integer> freespace : observationsFreespace) {
      switch (freespace.getLeft()) {
        case GOLD:
          amount = myself.getGoldCapacity() - freespace.getRight();
          myself.setBackpackAmount(Observation.GOLD, amount);
          break;
        case DIAMOND:
          amount = myself.getDiamondCapacity() - freespace.getRight();
          myself.setBackpackAmount(Observation.DIAMOND, amount);
          break;
        default:
          break;
      }
    }
  }

  public synchronized void merge(SerializableBrain serializedBrain) {
    map.mergeWithReceivedMap(serializedBrain.getGraph());
    entities.mergeAgents(serializedBrain.getAgents());
    entities.mergeTreasures(serializedBrain.getTreasures());
    entities.mergeSilos(serializedBrain.getSilos());
    entities.mergeGolems(serializedBrain.getGolems());
  }

  public synchronized void log(Object... args) {
    StringBuilder message = new StringBuilder();

    String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    message.append(timestamp)
        .append(" - ")
        .append(this.name)
        .append(" - ");

    if (args.length > 0) {
      message.append(args[0]);
      for (int i = 1; i < args.length; i++) {
        message.append(" ").append(args[i]);
      }
    }

    System.out.println(message);
  }

  public synchronized void notifyVisualization() {
    if (this.visualization != null) {
      this.visualization.updateFromModel();
    }
  }

  public synchronized void createVisualization() {
    this.visualization = new BrainVisualization(this, this.name);

    if (this.visualization.initialize()) {
      notifyVisualization();
    }
  }

  public synchronized void beforeMove() {
    this.map.beforeMove();
  }

  public synchronized void afterMove() {
    this.map.afterMove();
  }
}
