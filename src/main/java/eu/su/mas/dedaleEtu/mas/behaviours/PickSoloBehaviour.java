package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.Agent;

public class PickSoloBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1231959282640838272L;

  private Brain brain;
  private int exitValue;

  public PickSoloBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Pick Solo");

    int picked = ((AbstractDedaleAgent) this.myAgent).pick();

    System.out.println(this.myAgent.getLocalName() + " TREASURE PICKED = " + picked);
    this.exitValue = 1;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
