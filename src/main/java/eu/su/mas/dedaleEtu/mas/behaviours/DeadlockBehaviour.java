package eu.su.mas.dedaleEtu.mas.behaviours;

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
    brain.observe(this.myAgent);
    String goal = brain.findRandomNode();
    brain.log("i was stuck so im going to", goal);
    brain.mind.setTargetNode(goal);
    brain.mind.resetStuckCounter();
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
