package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

// Main exploration behaviour
public class CollectBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Brain brain;
  private int exitValue;

  public CollectBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    this.brain.mind.updateBehaviouralPriorities();

    if (!this.brain.mind.isCollectionPreferred()) {
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
