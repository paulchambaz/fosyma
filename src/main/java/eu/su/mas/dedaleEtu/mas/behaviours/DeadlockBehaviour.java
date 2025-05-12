package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Random;

import org.graphstream.graph.Graph;

import java.util.ArrayList;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedaleEtu.princ.Protocols;

public class DeadlockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -324667573071453845L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public DeadlockBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.observe(this.myAgent);

    Communication comms = Protocols.handshake(this.myAgent, brain, 100, "pleasemove", 10);
    if (comms != null) {
      return;
    }

    String position = brain.entities.getPosition();
    List<String> occupiedPositions = brain.entities.getOccupiedPositions();
    int maxDistance = Math.max(brain.mind.getStuckCounter(), 15);

    String goal = findNodeWithinDistance(position, occupiedPositions, maxDistance);
    brain.log("deadlock going to", goal);

    if (goal != null) {
      brain.mind.setTargetNode(goal);
      brain.mind.resetStuckCounter();
    } else {
      goal = brain.findRandomNode();
      if (goal != null) {
        brain.mind.setTargetNode(goal);
        brain.mind.resetStuckCounter();
      }
    }

    this.exitValue = 0;
  }

  private String findNodeWithinDistance(String startPosition, List<String> occupiedPositions, int maxDistance) {
    Graph navigableGraph = brain.map.createNavigableGraph(occupiedPositions);

    List<String> allNodeIds = new ArrayList<>();
    navigableGraph.nodes().forEach(node -> allNodeIds.add(node.getId()));

    List<String> reachableNodes = new ArrayList<>();

    for (String nodeId : allNodeIds) {
      if (nodeId.equals(startPosition)) {
        continue;
      }

      List<String> path = brain.map.findShortestPath(startPosition, nodeId, occupiedPositions);

      if (path != null && path.size() <= maxDistance) {
        reachableNodes.add(nodeId);
      }
    }

    if (!reachableNodes.isEmpty()) {
      Random random = new Random();
      int index = random.nextInt(reachableNodes.size());
      return reachableNodes.get(index);
    }

    return null;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
