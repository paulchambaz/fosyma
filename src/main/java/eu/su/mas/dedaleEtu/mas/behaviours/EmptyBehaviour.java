package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class EmptyBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7364592847383945821L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private int counter;

  private Brain brain;

  public EmptyBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  private void initialize() {
    counter = 10;

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    if (!this.initialized) {
      initialize();
    }

    this.brain.observe(this.myAgent);

    if (counter <= 0) {
      initialized = false;
      this.exitValue = 1;
      return;
    }
    counter--;

    Utils.waitFor(myAgent, 400);

    if (brain.mind.getMetaTargetNode() != null) {
      brain.mind.setTargetNode(brain.mind.getMetaTargetNode());
    }

    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
