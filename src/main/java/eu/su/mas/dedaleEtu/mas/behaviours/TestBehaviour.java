package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class TestBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Knowledge knowledge;
  private int exitValue;

  public TestBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
    this.exitValue = 0;
  }

  @Override
  public void action() {
    System.out.println("Test behaviour for " + this.myAgent.getLocalName());
    this.exitValue = 1;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
