package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class EmptyBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7364592847383945821L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public EmptyBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
