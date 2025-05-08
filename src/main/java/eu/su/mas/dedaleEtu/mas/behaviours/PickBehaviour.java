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

    brain.log("Picking a resource i am at : ", brain.entities.getPosition());
    int amount = ((AbstractDedaleAgent) this.myAgent).pick();
    brain.log("I picked up a total of", amount);
    TreasureData treasure = brain.entities.getTreasures().get(brain.entities.getPosition());
    if (treasure != null && amount > 0) {
      treasure.decreaseQuantity(amount);
      brain.entities.getMyself().increaseBackpack(amount);
    }

    this.brain.observe(this.myAgent);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
