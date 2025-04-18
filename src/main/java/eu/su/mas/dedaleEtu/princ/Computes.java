package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedaleEtu.mas.knowledge.SiloData;
import eu.su.mas.dedaleEtu.mas.knowledge.GolemData;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

public class Computes {
  public static double calculateNodeConnectivity(WorldMap map, String nodeId, int order) {
    if (order <= 0) {
      return 0.0;
    }

    Graph graph = map.getGraph();
    Node node = graph.getNode(nodeId);

    if (node == null) {
      return 0.0;
    }

    double connectivy = node.getDegree();

    if (order > 1) {
      Collection<Node> neighbours = node.neighborNodes().collect(Collectors.toList());
      for (Node neighbour : neighbours) {
        connectivy += calculateNodeConnectivity(map, neighbour.getId(), order - 1);
      }
    }

    return connectivy;
  }

  public static Map<String, Integer> calculateNodeDistancesToTreasures(WorldMap map, EntityTracker entities) {
    return new HashMap<>();
  }

  public static Map<String, Integer> calculateNodeDistancesToTreasures(
      WorldMap map, String nodeId, List<String> treasureLocations) {
    Map<String, Integer> distances = new HashMap<>();

    for (String treasureLocation : treasureLocations) {
      List<String> path = map.findShortestPath(nodeId, treasureLocation, new ArrayList<>());
      int distance = (path != null) ? path.size() : Integer.MAX_VALUE;
      distances.put(treasureLocation, distance);
    }

    return distances;
  }

  // General-purpose min-max weighted normalized regret solver.
  // Finds the item with the lowest maximum weighted normalized regret across all
  // criteria.
  public static int solveMinMaxRegret(double[][] criteriaValues, double[] weights) {
    int numItems = criteriaValues.length;
    int numCriteria = criteriaValues[0].length;

    // 1. find the ideal (minimum) value for each criterion
    double[] idealPoints = new double[numCriteria];
    Arrays.fill(idealPoints, Double.MAX_VALUE);
    for (int i = 0; i < numItems; i++) {
      for (int j = 0; j < numCriteria; j++) {
        idealPoints[j] = Math.min(idealPoints[j], criteriaValues[i][j]);
      }
    }

    // 2. calculate regret for each item and criterion (x_i - x*)
    double[][] regrets = new double[numItems][numCriteria];
    for (int i = 0; i < numItems; i++) {
      for (int j = 0; j < numCriteria; j++) {
        regrets[i][j] = criteriaValues[i][j] - idealPoints[j];
      }
    }

    // 3. find maximum regret for each criterion across all items
    double[] maxRegrets = new double[numCriteria];
    for (int j = 0; j < numCriteria; j++) {
      for (int i = 0; i < numItems; i++) {
        maxRegrets[j] = Math.max(maxRegrets[j], regrets[i][j]);
      }
    }

    // 4. normalize and weight regrets
    double[][] weightedNormalizedRegrets = new double[numItems][numCriteria];
    for (int i = 0; i < numItems; i++) {
      for (int j = 0; j < numCriteria; j++) {
        if (maxRegrets[j] > 0) {
          weightedNormalizedRegrets[i][j] = weights[j] * regrets[i][j] / maxRegrets[j];
        } else {
          weightedNormalizedRegrets[i][j] = 0;
        }
      }
    }

    // 5. for each item, find its worst (maximum) weighted normalized regret
    double[] scores = new double[numItems];
    for (int i = 0; i < numItems; i++) {
      double maxRegret = Double.NEGATIVE_INFINITY;
      for (int j = 0; j < numCriteria; j++) {
        maxRegret = Math.max(maxRegret, weightedNormalizedRegrets[i][j]);
      }
      scores[i] = maxRegret;
    }

    // 6. find the item with the minimum worst-case regret
    int bestIndex = 0;
    double minScore = scores[0];

    for (int i = 1; i < numItems; i++) {
      if (scores[i] < minScore) {
        minScore = scores[i];
        bestIndex = i;
      }
    }

    return bestIndex;
  }

  public static List<String> getNeighborhoodAgents(WorldMap map, EntityTracker entities, String currentNodeId) {
    List<String> neighborhoodAgents = new ArrayList<>();

    Graph graph = map.getGraph();
    Node currentNode = graph.getNode(currentNodeId);

    if (currentNode == null) {
      return neighborhoodAgents;
    }

    Collection<Node> neighbors = currentNode.neighborNodes().collect(Collectors.toList());

    List<String> neighborNodeIds = neighbors.stream()
        .map(Node::getId)
        .collect(Collectors.toList());

    Map<String, AgentData> agents = entities.getAgents();
    for (Map.Entry<String, AgentData> entry : agents.entrySet()) {
      String agentName = entry.getKey();
      String agentPosition = entry.getValue().getPosition();

      if (neighborNodeIds.contains(agentPosition)) {
        neighborhoodAgents.add(agentName);
      }
    }

    SiloData silo = entities.getSilo();
    if (silo != null && neighborNodeIds.contains(silo.getPosition())) {
      neighborhoodAgents.add("Silo");
    }

    GolemData golem = entities.getGolem();
    if (golem != null && neighborNodeIds.contains(golem.getPosition())) {
      neighborhoodAgents.add("Golem");
    }

    return neighborhoodAgents;
  }

  public static String findSearchedAgentInNeighborhood(WorldMap map, EntityTracker entities, String currentNodeId,
      List<String> searchingAgents) {
    List<String> neighborhoodAgents = getNeighborhoodAgents(map, entities, currentNodeId);

    for (String agent : neighborhoodAgents) {
      if (searchingAgents.contains(agent)) {
        return agent;
      }
    }

    return null;
  }
}
