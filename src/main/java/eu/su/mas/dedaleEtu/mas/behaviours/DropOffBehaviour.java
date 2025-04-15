package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.Agent;

public class DropOffBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1231959282640838272L;

  private Brain brain;
  private int exitValue;

  public DropOffBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  @Override
  public void action() {
    ((AbstractDedaleAgent) this.myAgent).dropOff();

    System.out.println(this.myAgent.getLocalName() + " DROPPED OFF");
    this.exitValue = 1;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
