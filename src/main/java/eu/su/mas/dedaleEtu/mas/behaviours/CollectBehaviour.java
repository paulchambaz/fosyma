package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
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
  private static final long serialVersionUID = -174647573271457865L;

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

    if (brain.entities.getMyself().getCapacity() > 0) {
      this.exitValue = 3;
      return;
    }

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
      this.exitValue = 1;
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  public String findOptimalTreasureNode(WorldMap map, EntityTracker entities, String currentPosition) {
    AgentData agent = entities.getMyself();
    Observation agentTreasureType = agent.getTreasureType();
    int myLockpickStrength = agent.getExpertise().getOrDefault(Observation.LOCKPICKING, 0);
    int myCarryStrength = agent.getExpertise().getOrDefault(Observation.STRENGH, 0);

    // Calculate max available strengths among all agents
    int totalLockpickStrength = myLockpickStrength;
    int totalCarryStrength = myCarryStrength;
    for (AgentData otherAgent : entities.getAgents().values()) {
      if (otherAgent != null && otherAgent.getExpertise() != null) {
        int lockpickStrength = otherAgent.getExpertise().getOrDefault(Observation.LOCKPICKING, 0);
        int carryStrength = otherAgent.getExpertise().getOrDefault(Observation.STRENGH, 0);
        totalLockpickStrength += lockpickStrength;
        totalCarryStrength += carryStrength;
      }
    }

    Map<String, TreasureData> treasures = entities.getTreasures();
    List<String> relevantTreasureNodes = new ArrayList<>();
    Map<String, Integer> treasureQuantities = new HashMap<>();
    Map<String, Boolean> canOpenLock = new HashMap<>();
    Map<String, Boolean> canCarryTreasure = new HashMap<>();
    Map<String, Integer> agentsNeededLockpick = new HashMap<>();
    Map<String, Integer> agentsNeededCarry = new HashMap<>();

    for (Map.Entry<String, TreasureData> entry : treasures.entrySet()) {
      TreasureData treasure = entry.getValue();
      if (treasure.getQuantity() <= 0) {
        continue;
      }
      if (treasure.getType() != agentTreasureType) {
        continue;
      }
      if (treasure.isLocked() && treasure.getLockpickStrength() > totalLockpickStrength) {
        continue;
      }
      if (treasure.getCarryStrength() > totalCarryStrength) {
        continue;
      }

      relevantTreasureNodes.add(entry.getKey());
      treasureQuantities.put(entry.getKey(), treasure.getQuantity());

      boolean canOpen = !treasure.isLocked() || agent.canOpenLock(treasure.getLockpickStrength());
      canOpenLock.put(entry.getKey(), canOpen);

      boolean canCarry = agent.canCarryTreasure(treasure.getCarryStrength());
      canCarryTreasure.put(entry.getKey(), canCarry);

      int agentsForLockpick = 0;
      int agentsForCarry = 0;

      if (!canOpen && treasure.isLocked()) {
        int requiredStrength = treasure.getLockpickStrength();
        int remainingStrength = requiredStrength - myLockpickStrength;

        if (remainingStrength > 0) {
          List<AgentData> sortedAgents = entities.getAgents().values().stream()
              .filter(a -> a != null && a.getExpertise() != null)
              .sorted((a1, a2) -> a2.getExpertise().getOrDefault(Observation.LOCKPICKING, 0) -
                  a1.getExpertise().getOrDefault(Observation.LOCKPICKING, 0))
              .collect(Collectors.toList());

          for (AgentData otherAgent : sortedAgents) {
            int agentStrength = otherAgent.getExpertise().getOrDefault(Observation.LOCKPICKING, 0);
            if (agentStrength > 0 && remainingStrength > 0) {
              agentsForLockpick++;
              remainingStrength -= agentStrength;
              if (remainingStrength <= 0)
                break;
            }
          }
        }
      }

      if (!canCarry) {
        int requiredStrength = treasure.getCarryStrength();
        int remainingStrength = requiredStrength - myCarryStrength;

        if (remainingStrength > 0) {
          List<AgentData> sortedAgents = entities.getAgents().values().stream()
              .filter(a -> a != null && a.getExpertise() != null)
              .sorted((a1, a2) -> a2.getExpertise().getOrDefault(Observation.STRENGH, 0) -
                  a1.getExpertise().getOrDefault(Observation.STRENGH, 0))
              .collect(Collectors.toList());

          for (AgentData otherAgent : sortedAgents) {
            int agentStrength = otherAgent.getExpertise().getOrDefault(Observation.STRENGH, 0);
            if (agentStrength > 0 && remainingStrength > 0) {
              agentsForCarry++;
              remainingStrength -= agentStrength;
              if (remainingStrength <= 0)
                break;
            }
          }
        }
      }

      agentsNeededLockpick.put(entry.getKey(), agentsForLockpick);
      agentsNeededCarry.put(entry.getKey(), agentsForCarry);
    }

    if (relevantTreasureNodes.isEmpty()) {
      return null;
    }

    Map<String, Integer> distances = Computes.calculateNodeDistancesToTreasures(map, currentPosition,
        relevantTreasureNodes);

    double[][] criteriaMatrix = new double[relevantTreasureNodes.size()][6];
    // brain.log("CHOOSING TREASURE");

    for (int i = 0; i < relevantTreasureNodes.size(); i++) {
      String nodeId = relevantTreasureNodes.get(i);
      Integer distance = distances.get(nodeId);
      Integer quantity = treasureQuantities.get(nodeId);
      Boolean canOpen = canOpenLock.get(nodeId);
      Boolean canCarry = canCarryTreasure.get(nodeId);
      Integer agentsForLockpick = agentsNeededLockpick.get(nodeId);
      Integer agentsForCarry = agentsNeededLockpick.get(nodeId);

      // brain.log(nodeId, distance, quantity, canOpen, canCarry, agentsForLockpick,
      // agentsForCarry);

      criteriaMatrix[i][0] = distance != null ? distance : Double.MAX_VALUE;
      criteriaMatrix[i][1] = -quantity;
      criteriaMatrix[i][2] = canOpen ? 0.0 : 1.0;
      criteriaMatrix[i][3] = canCarry ? 0.0 : 1.0;
      criteriaMatrix[i][4] = agentsForLockpick;
      criteriaMatrix[i][5] = agentsForCarry;
    }

    double[] weights = { 1.0, 4.0, 10.0, 10.0, 1.5, 1.5 };

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);
    return relevantTreasureNodes.get(bestIndex);
  }
}
