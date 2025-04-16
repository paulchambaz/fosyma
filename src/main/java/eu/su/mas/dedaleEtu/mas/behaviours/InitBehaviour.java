package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class InitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Brain brain;

  public InitBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Init");
    this.brain.createVisualization();
  }
}
