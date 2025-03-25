package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class EndBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -373863257632183865L;

  private Knowledge knowledge;

  public EndBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    System.out.println("End of " + this.myAgent.getLocalName());
  }
}
