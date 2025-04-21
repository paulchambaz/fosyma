package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class DeadlockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private int exitValue = 0;

  private Brain brain;

  public DeadlockBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Deadlock");
    brain.observe(this.myAgent);

    String goal;
    do {
      goal = brain.findRandomNode();

      String position = brain.entities.getPosition();

      List<String> path = brain.map.findShortestPath(position, goal, new ArrayList<>());
      if (!path.isEmpty()) {
        break;
      }
    } while (true);

    brain.log("i was stuck so im going to", goal);
    brain.mind.setTargetNode(goal);
    brain.mind.resetStuckCounter();
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
