package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.Agent;

public class ComputePathToSiloBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1231959282640838272L;

  private boolean initialized = false;

  private Brain brain;
  private int exitValue;

  public ComputePathToSiloBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  private void initialize() {
    System.out.println(this.myAgent.getLocalName() + " COMPUTE SILO");
    // this.brain.getMind().setGoal("SILO");

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
