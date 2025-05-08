package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class EndBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -373863257632183865L;

  private String state;
  private Brain brain;

  public EndBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    brain.log("end of the cycle");
  }
}
