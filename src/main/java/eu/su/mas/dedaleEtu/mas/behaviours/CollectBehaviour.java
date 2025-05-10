package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class CollectBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private String state;
  private int exitValue;

  private Brain brain;

  public CollectBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.mind.updateBehaviouralPriorities();

    if (!brain.mind.isCollectionPreferred() && brain.map.hasOpenNode()) {
      this.exitValue = 2;
      return;
    }

    brain.observe(this.myAgent);

    String currentPosition = brain.entities.getPosition();
    String goal = findOptimalTreasureNode(brain.map, brain.entities, currentPosition);

    if (goal != null) {
      brain.log("Going to collect treasure at", goal);
      brain.mind.setTargetNode(goal);
      this.exitValue = 0;
    } else {
      brain.log("No suitable treasure found to collect");
      // TODO: if there are no more treasure AND we have explored the entire
      // map then it really is the end, for now we just return to exploration
      // but it will return here ad infinite unless we just stopped because
      // collection changed
      this.exitValue = 1;
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  public String findOptimalTreasureNode(WorldMap map, EntityTracker entities, String currentPosition) {
    brain.log("CHOOSING A TREASURE TO SEEK");

    AgentData agent = entities.getMyself();
    Observation agentTreasureType = agent.getTreasureType();
    Map<String, TreasureData> treasures = entities.getTreasures();

    List<String> relevantTreasureNodes = new ArrayList<>();
    Map<String, Integer> treasureQuantities = new HashMap<>();
    Map<String, Boolean> canOpenLock = new HashMap<>();
    Map<String, Boolean> canCarryTreasure = new HashMap<>();

    // brain.log("Treasure type:", agent.getTreasureType(), "Carry strength:",
    // agent.getExpertise().get(Observation.STRENGH));

    // brain.log("here are the treasures i found");
    for (Map.Entry<String, TreasureData> entry : treasures.entrySet()) {
      TreasureData treasure = entry.getValue();
      // brain.log("Position:", entry.getKey(), "Quantity:", treasure.getQuantity(),
      // "Treasure type:", treasure.getType(),
      // "Carry strength:", treasure.getCarryStrength());
      // brain.log(entry.getKey());

      if (treasure.getQuantity() <= 0) {
        // brain.log(entry.getKey(), "quantity is 0 so skipping that treasure",
        // treasure.getQuantity());
        continue;
      }

      if (treasure.getType() != agentTreasureType) {
        // brain.log(entry.getKey(), "type is not of my type so skipping that treasure",
        // treasure.getType());
        continue;
      }

      // if (!agent.canCarryTreasure(treasure.getCarryStrength())) {
      // // brain.log(entry.getKey(), "i cant pick that treasure so skip it",
      // // treasure.getType());
      // continue;
      // }

      relevantTreasureNodes.add(entry.getKey());
      treasureQuantities.put(entry.getKey(), treasure.getQuantity());

      boolean canOpen = !treasure.isLocked() || agent.canOpenLock(treasure.getLockpickStrength());
      canOpenLock.put(entry.getKey(), canOpen);

      boolean canCarry = !agent.canCarryTreasure(treasure.getCarryStrength());
      canCarryTreasure.put(entry.getKey(), canCarry);
    }

    if (relevantTreasureNodes.isEmpty()) {
      return null;
    }

    Map<String, Integer> distances = Computes.calculateNodeDistancesToTreasures(map, currentPosition,
        relevantTreasureNodes);

    double[][] criteriaMatrix = new double[relevantTreasureNodes.size()][4];
    for (int i = 0; i < relevantTreasureNodes.size(); i++) {
      String nodeId = relevantTreasureNodes.get(i);
      Integer distance = distances.get(nodeId);
      Integer quantity = treasureQuantities.get(nodeId);
      Boolean canOpen = canOpenLock.get(nodeId);
      Boolean canCarry = canCarryTreasure.get(nodeId);

      // brain.log(nodeId, distance, quantity, canOpen, canCarry);

      criteriaMatrix[i][0] = distance != null ? distance : Double.MAX_VALUE;
      criteriaMatrix[i][1] = -quantity;
      criteriaMatrix[i][2] = canOpen ? 0.0 : 1.0;
      criteriaMatrix[i][3] = canCarry ? 0.0 : 1.0;
    }

    double[] weights = { 2.0, 1.0, 4.0, 4.0 };

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);

    return relevantTreasureNodes.get(bestIndex);
  }
}
