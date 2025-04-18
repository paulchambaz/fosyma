package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class DropoffBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -383953836736159459L;

  private int exitValue = 0;

  private Brain brain;

  public DropoffBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Dropoff");

    AbstractDedaleAgent agent = ((AbstractDedaleAgent) this.myAgent);

    if (agent.emptyMyBackPack("Silo")) {
      this.exitValue = 0;
    } else {
      this.exitValue = 1;
    }

    this.brain.observe(this.myAgent);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
