package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class PickBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -383953874837483245L;

  private int exitValue = 0;

  private Brain brain;

  public PickBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Pick");
    brain.observe(this.myAgent);

    ((AbstractDedaleAgent) this.myAgent).pick();

    this.brain.observe(this.myAgent);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
