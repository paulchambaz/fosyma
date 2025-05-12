package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class TryPickBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7376583127383945821L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private int counter;

  private Brain brain;

  public TryPickBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  private void initialize() {
    counter = 20;

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.log(brain.entities.getPosition());

    if (!initialized) {
      initialize();
    }

    this.brain.observe(this.myAgent);

    if (counter <= 0) {
      initialized = false;
      this.exitValue = 2;
      return;
    }
    counter--;

    int amount = ((AbstractDedaleAgent) this.myAgent).pick();
    brain.log("tried to pick, result:", amount);
    if (amount > 0) {
      this.brain.observe(this.myAgent);

      initialized = false;
      this.exitValue = 1;
      return;
    }

    Utils.waitFor(myAgent, 400);

    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
