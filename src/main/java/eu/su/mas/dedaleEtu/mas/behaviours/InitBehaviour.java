package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class InitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -3746237553871487865L;

  private String state;
  private Brain brain;

  public InitBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.createVisualization();

    brain.observe(this.myAgent);
    brain.selfLearn(this.myAgent);
  }
}
