package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.graphstream.graph.Graph;

import java.util.HashMap;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.princ.Computes;

public class CollectSiloBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private String state;
  private int exitValue;

  private Brain brain;

  public CollectSiloBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    brain.mind.updateBehaviouralPriorities();

    if (brain.map.hasOpenNode()) {
      this.exitValue = 2;
      return;
    }

    brain.observe(this.myAgent);

    String goal = findOptimalWaitingNode(brain.map, brain.entities, 2.0);

    brain.log("finished, going to wait in", goal);
    brain.mind.setTargetNode(goal);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
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

  // Calculates nth-order connectivity for each node in the graph.
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

  // Computes shortest path distances from each node to each treasure.
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

}
