package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

// Main exploration behaviour
public class CollectBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Knowledge knowledge;
  private int exitValue;

  public CollectBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    // System.out.println("Collecting");

    this.knowledge.updateDesireExplore();

    if (!this.knowledge.wantsToCollect()) {
      this.exitValue = 2;
      return;
    }

    this.exitValue = 1;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
