package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class RestoreTargetBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7839428374629384726L;
  private int exitValue = 0;
  private Brain brain;

  public RestoreTargetBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Restore Target");

    String metaTarget = brain.mind.getMetaTargetNode();

    if (metaTarget != null && !metaTarget.isEmpty()) {
      brain.log("Restoring target from meta target:", metaTarget);
      brain.mind.setTargetNode(metaTarget);
      this.exitValue = 1;
    } else {
      brain.log("No meta target to restore");
      this.exitValue = 0;
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
