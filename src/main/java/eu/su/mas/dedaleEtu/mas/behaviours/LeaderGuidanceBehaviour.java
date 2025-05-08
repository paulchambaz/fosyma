package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;

public class LeaderGuidanceBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -3284756938273645L;
  private int exitValue = 0;
  private Brain brain;

  private static final int CHUNK_SIZE = 3;

  public LeaderGuidanceBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Leader Guidance");

    if (brain.mind.getCoordinationState() != CoordinationState.LEADER) {
      brain.log("Error: Not in leader state");
      this.exitValue = 0;
      return;
    }

    String followerName = brain.mind.getCoordinationPartner();
    if (followerName == null) {
      brain.log("Error: No follower assigned");
      resetCoordination();
      this.exitValue = 0;
      return;
    }

    // Get the target treasure node
    String treasureNode = brain.mind.getMetaTargetNode();
    if (treasureNode == null) {
      brain.log("Error: No target treasure node set");
      resetCoordination();
      this.exitValue = 0;
      return;
    }

    String currentPosition = brain.entities.getPosition();

    List<String> pathToTreasure = brain.map.findShortestPath(
        currentPosition, treasureNode, new ArrayList<>());

    if (pathToTreasure == null || pathToTreasure.isEmpty()) {
      brain.log("Error: No path found to treasure at", treasureNode);
      resetCoordination();
      this.exitValue = 0;
      return;
    }

    brain.mind.setPathToTarget(new ArrayList<>(pathToTreasure));

    String waypoint = selectWaypoint(pathToTreasure);
    brain.log("Selected waypoint", waypoint, "on path to treasure");

    brain.mind.setTargetNode(waypoint);

    this.exitValue = 1;
  }

  private String selectWaypoint(List<String> path) {
    if (path.size() <= CHUNK_SIZE) {
      return path.get(path.size() - 1);
    }

    return path.get(CHUNK_SIZE - 1);
  }

  private void resetCoordination() {
    brain.mind.setCoordinationState(CoordinationState.NONE);
    brain.mind.setCoordinationPartner(null);
    brain.mind.setCoordinationTreasureNode(null);
    brain.mind.setMetaTargetNode(null);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
