package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class PickBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -383953874837483245L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public PickBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.observe(this.myAgent);

    String position = brain.entities.getPosition();
    TreasureData treasure = brain.entities.getTreasures().get(position);

    if (treasure == null) {
      this.exitValue = 2;
      return;
    }

    brain.log("Trying to pick treasure in", position);
    int amount = ((AbstractDedaleAgent) this.myAgent).pick();
    if (amount > 0) {
      treasure.decreaseQuantity(amount);
      brain.entities.getMyself().increaseBackpack(amount);
      this.brain.observe(this.myAgent);

      brain.log("success");
      this.exitValue = 0;
      return;
    }
    brain.log("failure");

    brain.mind.setCoordinationTreasureNode(position);
    this.exitValue = 1;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
