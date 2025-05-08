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

  public static String computeMyMeetingPoint(WorldMap map, double opennessWeight, double distanceWeight, String position){
    Map<String, double[]> nodeCriteriaValues = calculateNodeCriteriaValues(map, position);

    double[] weights = new double[2];
    weights[0] = opennessWeight;
    weights[1] = distanceWeight;

    List<String> nodeIds = new ArrayList<>(nodeCriteriaValues.keySet());
    double[][] criteriaMatrix = new double[nodeIds.size()][weights.length];

    for (int i = 0; i < nodeIds.size(); i++) {
      criteriaMatrix[i] = nodeCriteriaValues.get(nodeIds.get(i));
    }
    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);
    return nodeIds.get(bestIndex);
  }

  public static Map<String, double[]> calculateNodeCriteriaValues(WorldMap map, String position) {
    Map<String, double[]> results = new HashMap<>();
    Map<String, Double> connectivityMap = calculateNodesConnectivity(map, 3);
    Map<String, Integer> distancesMap = calculateNodesDistancesToPosition(map, position);

    for (String nodeId : connectivityMap.keySet()) {
      double[] criteria = new double[2];
      double connectivity = connectivityMap.get(nodeId);
      criteria[0] = -connectivity;

      Integer distance = distancesMap.get(nodeId);
      criteria[1] = (distance != null && distance < Integer.MAX_VALUE) ? distance : Double.MAX_VALUE;

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

  public static Map<String, Integer> calculateNodesDistancesToPosition(WorldMap map, String position) {
    Map<String, Integer> results = new HashMap<>();
    Graph graph = map.getGraph();

    graph.nodes().forEach(node -> {
      String nodeId = node.getId();
      
      List<String> path = map.findShortestPath(nodeId, position, new ArrayList<>());
      int distancesToPosition = (path != null) ? path.size() : Integer.MAX_VALUE;

      results.put(nodeId, distancesToPosition);
    });

    return results;
  }

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

    Map<String, SiloData> silos = entities.getSilos();
    for (Map.Entry<String, SiloData> entry : silos.entrySet()) {
      String siloName = entry.getKey();
      String siloPosition = entry.getValue().getPosition();

      if (neighborNodeIds.contains(siloPosition)) {
        neighborhoodAgents.add(siloName);
      }
    }

    Map<String, GolemData> golems = entities.getGolems();
    for (Map.Entry<String, GolemData> entry : golems.entrySet()) {
      String golemName = entry.getKey();
      String golemPosition = entry.getValue().getPosition();

      if (neighborNodeIds.contains(golemPosition)) {
        neighborhoodAgents.add(golemName);
      }
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

  public static double[] toSoftmax(double[] values) {
    double max = Arrays.stream(values).max().orElse(0);

    double[] exps = new double[values.length];
    double sum = 0;
    for (int i = 0; i < values.length; i++) {
      exps[i] = Math.exp(values[i] - max);
      sum += exps[i];
    }

    double[] probabilities = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      probabilities[i] = exps[i] / sum;
    }

    return probabilities;
  }

  public static List<Integer> sampleFromDistribution(double[] probabilities, int n) {
    int size = Math.min(n, probabilities.length);
    List<Integer> result = new ArrayList<>(size);

    double[] remainingProbs = Arrays.copyOf(probabilities, probabilities.length);

    for (int i = 0; i < size; i++) {
      double sum = Arrays.stream(remainingProbs).sum();
      double rand = Math.random() * sum;

      double cumulativeProb = 0;
      int selectedIndex = -1;

      for (int j = 0; j < remainingProbs.length; j++) {
        cumulativeProb += remainingProbs[j];
        if (cumulativeProb >= rand) {
          selectedIndex = j;
          break;
        }
      }

      result.add(selectedIndex);
      remainingProbs[selectedIndex] = 0;
    }

    return result;
  }
}
