package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import jade.core.Agent;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.mas.knowledge.WorldMap;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedaleEtu.mas.knowledge.EntityTracker;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class MoveAsideBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Brain brain;
  private int exitValue;

  public MoveAsideBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Move Aside");
    brain.observe(this.myAgent);

    String currentPosition = brain.entities.getPosition();
    String goal = findOptimalNode(brain.map, brain.entities, currentPosition);

    if (goal != null) {
      brain.log("Moving aside to", goal);
      brain.mind.setTargetNode(goal);
      this.exitValue = 0;
    } else {
      brain.log("No positions found to move aside to.");
      this.exitValue = 1;
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  public static String findOptimalNode(WorldMap map, EntityTracker entities, String currentPosition) {
    // get graph without entities on it
    List<String> occupiedPositions = entities.getOccupiedPositions();
    // get connected component of actual agent
    // ConnectedComponents.ConnectedComponent cc = map.getMyConnectedComponent(occupiedPositions);
    // Set<org.graphstream.graph.Node> 
    // pick a node in connected component
    // return Computes.computeMyMeetingPoint(cc, 0.5, 2, currentPosition);
    return currentPosition;
  }
}
