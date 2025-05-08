package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedale.env.Observation;

public class CoordinationInitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -5873940328749320578L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public CoordinationInitBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    String treasureNode = brain.mind.getCoordinationTreasureNode();
    if (treasureNode == null) {
      this.exitValue = 0;
      return;
    }

    TreasureData treasure = brain.entities.getTreasures().get(treasureNode);
    if (treasure == null) {
      brain.log("Treasure no longer exists at node:", treasureNode);
      this.exitValue = 0;
      return;
    }

    int requiredStrength = treasure.getLockpickStrength()
        - brain.entities.getMyself().getExpertise().get(Observation.LOCKPICKING);

    List<String> capableAgents = brain.entities.getAgentsWithLockpickingStrength(requiredStrength);
    if (capableAgents.isEmpty()) {
      brain.log("No capable agents found to help open treasure");
      this.exitValue = 0;
      return;
    }

    Map<String, Double> proximityScores = calculateProximityScores(capableAgents, treasureNode);
    if (proximityScores.isEmpty()) {
      brain.log("No reachable capable agents found");
      this.exitValue = 2;
      return;
    }

    String selectedAgent = selectClosestAgent(proximityScores);
    if (selectedAgent == null) {
      brain.log("Failed to select a coordination partner");
      this.exitValue = 0;
      return;
    }

    brain.mind.setCoordinationPartner(selectedAgent);
    brain.mind.setMetaTargetNode(treasureNode);
    brain.mind.setCoordinationState(CoordinationState.LEADER);
    brain.log("Selected agent", selectedAgent, "for coordination to open treasure at", treasureNode);

    String meetingPoint = brain.entities.getAgentMeetingPoint(selectedAgent);
    if (meetingPoint == null) {
      brain.log("No meeting point set for agent:", selectedAgent);
      String agentPosition = brain.entities.getAgents().get(selectedAgent).getPosition();
      if (agentPosition != null) {
        brain.mind.setTargetNode(agentPosition);
        this.exitValue = 1;
      } else {
        brain.log("Unable to locate agent:", selectedAgent);
        this.exitValue = 0;
      }
    } else {
      brain.mind.setTargetNode(meetingPoint);
      brain.log("going to meeting point", meetingPoint);
      this.exitValue = 1;
    }
  }

  private Map<String, Double> calculateProximityScores(List<String> agentNames, String treasureNode) {
    Map<String, Double> scores = new HashMap<>();
    String currentPosition = brain.entities.getPosition();

    for (String agentName : agentNames) {
      AgentData agent = brain.entities.getAgents().get(agentName);
      if (agent == null || agent.getPosition() == null) {
        continue;
      }

      // Calculate distance to agent
      List<String> pathToAgent = brain.map.findShortestPath(
          currentPosition, agent.getPosition(), new ArrayList<>());
      if (pathToAgent == null) {
        continue;
      }

      // Calculate distance from agent to treasure
      List<String> pathToTreasure = brain.map.findShortestPath(
          agent.getPosition(), treasureNode, new ArrayList<>());
      if (pathToTreasure == null) {
        continue;
      }

      double distanceScore = pathToAgent.size() + pathToTreasure.size();

      TreasureData treasure = brain.entities.getTreasures().get(treasureNode);
      int requiredStrength = treasure.getLockpickStrength();
      int agentStrength = 0;
      Map<Observation, Integer> expertise = agent.getExpertise();
      if (expertise != null && expertise.containsKey(Observation.LOCKPICKING)) {
        agentStrength = expertise.get(Observation.LOCKPICKING);
      }

      double strengthBonus = (agentStrength > requiredStrength) ? 0.8 : 1.0;

      scores.put(agentName, distanceScore * strengthBonus);
    }

    return scores;
  }

  private String selectClosestAgent(Map<String, Double> proximityScores) {
    return proximityScores.entrySet().stream()
        .min(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
