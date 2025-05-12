package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import org.graphstream.graph.Graph;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.princ.Computes;

public class ComputeEndPositionBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -98745638789632541L;

  private String state;
  private int exitValue;

  private Brain brain;

  public ComputeEndPositionBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    brain.observe(this.myAgent);

    if (brain.entities.getMyself().getCapacity() > 0) {
      this.exitValue = 1;
      return;
    }

    String goal = findOptimalEndPosition(brain.map, brain.entities, 3.0, 1.5);

    brain.log("computed final position at", goal);
    brain.mind.setTargetNode(goal);

    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  public String findOptimalEndPosition(WorldMap map, EntityTracker entities, double opennessWeight,
      double balancedDistanceWeight) {

    Set<String> treasureLocations = new HashSet<String>(entities.getTreasures().keySet());

    Map<String, double[]> nodeCriteriaValues = calculateNodeEndPositionCriteria(map, entities);

    Map<String, double[]> filteredCriteriaValues = new HashMap<>();
    for (Map.Entry<String, double[]> entry : nodeCriteriaValues.entrySet()) {
      if (!treasureLocations.contains(entry.getKey())) {
        filteredCriteriaValues.put(entry.getKey(), entry.getValue());
      }
    }

    if (filteredCriteriaValues.isEmpty()) {
      brain.log("Warning: All potential end positions contain treasures. Using fallback logic.");
      filteredCriteriaValues = nodeCriteriaValues;
    }

    double[] weights = calculateEndPositionWeights(entities, opennessWeight, balancedDistanceWeight);

    List<String> nodeIds = new ArrayList<>(filteredCriteriaValues.keySet());
    double[][] criteriaMatrix = new double[nodeIds.size()][weights.length];

    for (int i = 0; i < nodeIds.size(); i++) {
      criteriaMatrix[i] = filteredCriteriaValues.get(nodeIds.get(i));
    }

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);

    return nodeIds.get(bestIndex);
  }

  public static Map<String, double[]> calculateNodeEndPositionCriteria(WorldMap map, EntityTracker entities) {
    Map<String, double[]> results = new HashMap<>();

    Map<String, Double> connectivityMap = calculateNodesConnectivity(map, 4);
    Map<String, Map<String, Integer>> distancesMap = calculateNodesDistancesToTreasures(map, entities);
    Map<String, Double> balancedDistanceMap = calculateBalancedDistances(distancesMap, entities);

    List<String> treasureLocations = new ArrayList<>(entities.getTreasures().keySet());
    for (String nodeId : connectivityMap.keySet()) {
      double[] criteria = new double[2 + treasureLocations.size()];

      double connectivity = connectivityMap.get(nodeId);
      criteria[0] = -connectivity;

      criteria[1] = balancedDistanceMap.get(nodeId);

      Map<String, Integer> distances = distancesMap.get(nodeId);
      for (int i = 0; i < treasureLocations.size(); i++) {
        String treasureLocation = treasureLocations.get(i);
        Integer distance = distances.get(treasureLocation);

        criteria[i + 2] = (distance != null && distance < Integer.MAX_VALUE) ? distance : Double.MAX_VALUE;
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

  public static Map<String, Double> calculateBalancedDistances(Map<String, Map<String, Integer>> distancesMap,
      EntityTracker entities) {
    Map<String, Double> results = new HashMap<>();
    Map<String, TreasureData> treasures = entities.getTreasures();

    int maxDistance = 0;
    for (Map<String, Integer> distances : distancesMap.values()) {
      for (Integer dist : distances.values()) {
        if (dist != null && dist < Integer.MAX_VALUE) {
          maxDistance = Math.max(maxDistance, dist);
        }
      }
    }

    double idealLowerBound = maxDistance * 0.4;
    double idealUpperBound = maxDistance * 0.6;

    for (String nodeId : distancesMap.keySet()) {
      Map<String, Integer> distances = distancesMap.get(nodeId);
      double balanceScore = 0;
      int validDistances = 0;

      for (String treasureLocation : treasures.keySet()) {
        Integer distance = distances.get(treasureLocation);
        if (distance != null && distance < Integer.MAX_VALUE) {
          double distanceScore;
          if (distance < idealLowerBound) {
            distanceScore = (idealLowerBound - distance) / idealLowerBound;
          } else if (distance > idealUpperBound) {
            distanceScore = (distance - idealUpperBound) / (maxDistance - idealUpperBound);
          } else {
            distanceScore = 0;
          }
          balanceScore += distanceScore;
          validDistances++;
        }
      }

      results.put(nodeId, validDistances > 0 ? balanceScore / validDistances : Double.MAX_VALUE);
    }

    return results;
  }

  public static double[] calculateEndPositionWeights(EntityTracker entities, double opennessWeight,
      double balancedDistanceWeight) {
    Map<String, TreasureData> treasures = entities.getTreasures();
    int numTreasures = treasures.size();

    double[] weights = new double[numTreasures + 2];
    weights[0] = opennessWeight;
    weights[1] = balancedDistanceWeight;

    int maxQuantity = 0;
    for (TreasureData treasure : treasures.values()) {
      maxQuantity = Math.max(maxQuantity, treasure.getQuantity());
    }

    double epsilon = 0.01;

    int index = 2;
    for (TreasureData treasure : treasures.values()) {
      double normalizedWeight = maxQuantity > 0 ? 1.0 - ((double) treasure.getQuantity() / maxQuantity) + epsilon
          : epsilon;
      weights[index] = Math.max(normalizedWeight, epsilon);
      index++;
    }

    return weights;
  }
}
