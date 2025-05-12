package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.graphstream.graph.Graph;
import java.util.HashMap;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedale.env.Observation;

public class CoordinationLocateAgent extends OneShotBehaviour {
  private static final long serialVersionUID = -5873940328749320578L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private Brain brain;

  private boolean hasTriedMeetingPoint;
  private boolean hasTriedLastPosition;
  private boolean hasTriedSilo;

  private String selectedAgent;

  public CoordinationLocateAgent(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  private void initialize() {
    hasTriedMeetingPoint = false;
    hasTriedLastPosition = false;
    hasTriedSilo = false;

    String treasureNode = brain.mind.getCoordinationTreasureNode();
    if (treasureNode == null) {
      return;
    }

    List<String> coalition = selectCoalition(treasureNode);
    if (coalition == null) {
      brain.log("Could not form coalition for treasure at", treasureNode);
      this.exitValue = 0;
      return;
    }

    TreasureData treasure = brain.entities.getTreasures().get(treasureNode);
    if (treasure == null) {
      brain.log("Treasure no longer exists at node:", treasureNode);
      return;
    }

    brain.mind.setCoalitionMembers(coalition);
    brain.mind.setCoalitionMembersPresent(1);

    selectedAgent = selectBestAgent(coalition);
    if (selectedAgent == null) {
      brain.log("No suitable agent found for coordination");
      return;
    }

    brain.log("Selected agent from coalition:", selectedAgent);
    brain.mind.setCoordinationPartner(selectedAgent);
    brain.mind.setCoordinationState(CoordinationState.LEADER);

    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    this.brain.observe(this.myAgent);

    if (!initialized) {
      initialize();
    }

    if (selectedAgent == null) {
      brain.log("could not select agent");
      this.exitValue = 0;
      return;
    }

    brain.log("STATUS", hasTriedMeetingPoint, hasTriedLastPosition, hasTriedSilo);

    if (!hasTriedMeetingPoint) {
      brain.log("trying meeting point with", selectedAgent);
      String meetingPoint = brain.entities.getAgentMeetingPoint(selectedAgent);
      if (meetingPoint != null) {
        brain.log("we have a meeting point with", selectedAgent, "in", meetingPoint);
        brain.mind.setMetaTargetNode(meetingPoint);
        brain.mind.setTargetNode(meetingPoint);
        hasTriedMeetingPoint = true;
        this.exitValue = 1;
        return;
      }
    }

    if (!hasTriedLastPosition) {
      brain.log("trying last position with", selectedAgent);
      String agentPosition = brain.entities.getAgents().get(selectedAgent).getPosition();
      if (agentPosition != null) {
        brain.log("we have a last position with", selectedAgent, "in", agentPosition);
        brain.mind.setMetaTargetNode(agentPosition);
        brain.mind.setTargetNode(agentPosition);
        hasTriedLastPosition = true;
        this.exitValue = 1;
        return;
      }
    }

    if (!hasTriedSilo) {
      brain.log("trying silo position with", selectedAgent);
      String siloPosition = findOptimalWaitingNode(brain.map, brain.entities, 2.0);
      if (siloPosition != null) {
        brain.log("we have a silo position with", selectedAgent, "in", siloPosition);
        brain.mind.setMetaTargetNode(siloPosition);
        brain.mind.setTargetNode(siloPosition);
        hasTriedSilo = true;
        this.exitValue = 1;
        return;
      }
    }

    this.initialized = false;
    this.exitValue = 0;
  }

  private List<String> selectCoalition(String treasureNode) {
    TreasureData treasure = brain.entities.getTreasures().get(treasureNode);
    if (treasure == null) {
      brain.log("Treasure data not found for node:", treasureNode);
      return null;
    }

    AgentData myself = brain.entities.getMyself();

    int myLockpickStrength = myself.getExpertise().getOrDefault(Observation.LOCKPICKING, 0);
    int myCarryStrength = myself.getExpertise().getOrDefault(Observation.STRENGH, 0);

    int requiredLockpickStrength = treasure.getLockpickStrength() - myLockpickStrength;
    int requiredCarryStrength = treasure.getCarryStrength() - myCarryStrength;

    if (requiredLockpickStrength <= 0 && requiredCarryStrength <= 0) {
      return new ArrayList<>();
    }

    List<String> potentialMembers = new ArrayList<>(brain.entities.getAgents().keySet());

    List<String> coalition = new ArrayList<>();
    int coalitionLockpickStrength = 0;
    int coalitionCarryStrength = 0;

    potentialMembers.sort((a, b) -> {
      AgentData agentA = brain.entities.getAgents().get(a);
      AgentData agentB = brain.entities.getAgents().get(b);
      int skillA = agentA.getExpertise().getOrDefault(Observation.LOCKPICKING, 0) +
          agentA.getExpertise().getOrDefault(Observation.STRENGH, 0);
      int skillB = agentB.getExpertise().getOrDefault(Observation.LOCKPICKING, 0) +
          agentB.getExpertise().getOrDefault(Observation.STRENGH, 0);
      return Integer.compare(skillB, skillA);
    });

    for (String agentName : potentialMembers) {
      if (coalitionLockpickStrength >= requiredLockpickStrength && coalitionCarryStrength >= requiredCarryStrength) {
        break;
      }

      AgentData agent = brain.entities.getAgents().get(agentName);
      if (agent == null)
        continue;

      coalition.add(agentName);
      coalitionLockpickStrength += agent.getExpertise().getOrDefault(Observation.LOCKPICKING, 0);
      coalitionCarryStrength += agent.getExpertise().getOrDefault(Observation.STRENGH, 0);
    }

    if (coalitionLockpickStrength < requiredLockpickStrength || coalitionCarryStrength < requiredCarryStrength) {
      brain.log("Cannot form adequate coalition - insufficient skills");
      return null;
    }

    return coalition;
  }

  private String selectBestAgent(List<String> coalition) {
    if (coalition == null || coalition.isEmpty()) {
      return null;
    }

    String currentPosition = brain.entities.getPosition();
    Map<String, double[]> agentCriteriaValues = new HashMap<>();

    for (String agentName : coalition) {
      AgentData agent = brain.entities.getAgents().get(agentName);
      if (agent == null || agent.getPosition() == null) {
        continue;
      }

      double[] criteria = new double[3];

      // Criterion 1: Distance to agent (shorter is better)
      List<String> pathToAgent = brain.map.findShortestPath(
          currentPosition, agent.getPosition(), new ArrayList<>());
      criteria[0] = (pathToAgent != null) ? pathToAgent.size() : Double.MAX_VALUE;

      // Criterion 2: Data freshness (newer is better)
      criteria[1] = agent.getUpdateCounter();

      // Criterion 3: Existence of meeting point (having one is better)
      criteria[2] = (agent.getMeetingPoint() != null) ? 0 : 1;

      agentCriteriaValues.put(agentName, criteria);
    }

    if (agentCriteriaValues.isEmpty()) {
      return null;
    }

    // Weights for different criteria (distance, freshness, meeting point)
    double[] weights = { 2.0, 1.0, 0.5 };

    List<String> agentIds = new ArrayList<>(agentCriteriaValues.keySet());
    double[][] criteriaMatrix = new double[agentIds.size()][weights.length];

    for (int i = 0; i < agentIds.size(); i++) {
      criteriaMatrix[i] = agentCriteriaValues.get(agentIds.get(i));
    }

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);
    return agentIds.get(bestIndex);
  }

  public static String findOptimalWaitingNode(WorldMap map, EntityTracker entities, double opennessWeight) {
    Map<String, double[]> nodeCriteriaValues = calculateNodeCriteriaValues(map, entities);

    double[] weights = calculateCriteriaWeights(entities, opennessWeight);

    List<String> nodeIds = new ArrayList<>(nodeCriteriaValues.keySet());
    double[][] criteriaMatrix = new double[nodeIds.size()][weights.length];

    for (int i = 0; i < nodeIds.size(); i++) {
      criteriaMatrix[i] = nodeCriteriaValues.get(nodeIds.get(i));
    }

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);

    return nodeIds.get(bestIndex);
  }

  public static Map<String, double[]> calculateNodeCriteriaValues(WorldMap map, EntityTracker entities) {
    Map<String, double[]> results = new HashMap<>();

    Map<String, Double> connectivityMap = calculateNodesConnectivity(map, 3);
    Map<String, Map<String, Integer>> distancesMap = calculateNodesDistancesToTreasures(map, entities);

    List<String> treasureLocations = new ArrayList<>(entities.getTreasures().keySet());
    for (String nodeId : connectivityMap.keySet()) {
      double[] criteria = new double[1 + treasureLocations.size()];

      double connectivity = connectivityMap.get(nodeId);
      criteria[0] = -connectivity;

      Map<String, Integer> distances = distancesMap.get(nodeId);
      for (int i = 0; i < treasureLocations.size(); i++) {
        String treasureLocation = treasureLocations.get(i);
        Integer distance = distances.get(treasureLocation);

        criteria[i + 1] = (distance != null && distance < Integer.MAX_VALUE) ? distance : Double.MAX_VALUE;
      }

      results.put(nodeId, criteria);
    }

    return results;
  }

  public static Map<String, Double> calculateNodesConnectivity(WorldMap map, int order) {
    Map<String, Double> results = new HashMap<>();
    Graph graph = map.getGraph();

    graph.nodes().forEach(node -> {
      String nodeId = node.getId();
      double connectivity = Computes.calculateNodeConnectivity(map, nodeId, order);
      results.put(nodeId, connectivity);
    });

    return results;
  }

  public static Map<String, Map<String, Integer>> calculateNodesDistancesToTreasures(WorldMap map,
      EntityTracker entities) {
    Map<String, Map<String, Integer>> results = new HashMap<>();
    Graph graph = map.getGraph();

    Map<String, TreasureData> treasures = entities.getTreasures();
    List<String> treasureLocations = new ArrayList<>(treasures.keySet());

    graph.nodes().forEach(node -> {
      String nodeId = node.getId();
      Map<String, Integer> distancesToTreasures = Computes.calculateNodeDistancesToTreasures(map, nodeId,
          treasureLocations);
      results.put(nodeId, distancesToTreasures);
    });

    return results;
  }

  public static double[] calculateCriteriaWeights(EntityTracker entities, double opennessWeight) {
    Map<String, TreasureData> treasures = entities.getTreasures();
    int numTreasures = treasures.size();

    double[] weights = new double[numTreasures + 1];
    weights[0] = opennessWeight;

    int maxQuantity = 0;
    for (TreasureData treasure : treasures.values()) {
      maxQuantity = Math.max(maxQuantity, treasure.getQuantity());
    }

    double epsilon = 0.01;

    int index = 1;
    for (TreasureData treasure : treasures.values()) {
      double normalizeWeight = maxQuantity > 0 ? (double) treasure.getQuantity() / maxQuantity : epsilon;
      weights[index] = Math.max(normalizeWeight, epsilon);
      index++;
    }

    return weights;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
