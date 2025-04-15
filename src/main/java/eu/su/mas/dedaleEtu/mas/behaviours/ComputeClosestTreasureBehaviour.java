package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.Agent;

public class ComputeClosestTreasureBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1231959282640838272L;

  private boolean initialized = false;

  private Brain brain;
  private int exitValue;

  public ComputeClosestTreasureBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  private void initialize() {
    // this.brain.getMind().setGoal("TREASURE");
    this.initialized = true;
  }

  @Override
  public void action() {
    if (!initialized) {
      initialize();
      this.exitValue = 1;
    }
  }

  @Override
  public int onEnd() {
    this.initialized = false;
    return this.exitValue;
  }
}
