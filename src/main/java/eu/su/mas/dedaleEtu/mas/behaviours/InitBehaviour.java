package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class InitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Knowledge knowledge;

  public InitBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    System.out.println("Initializing " + this.myAgent.getLocalName());
  }
}
