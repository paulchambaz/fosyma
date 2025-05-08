package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class WaitUntilBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public WaitUntilBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    this.brain.observe(this.myAgent);
    this.brain.updateBackpack(this.myAgent);

    Utils.waitFor(this.myAgent, 1000);

    brain.entities.ageEntities();
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
