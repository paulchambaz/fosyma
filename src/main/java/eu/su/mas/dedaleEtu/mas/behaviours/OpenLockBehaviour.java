package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class OpenLockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -385943593446598313L;

  private int exitValue = 0;

  private Brain brain;

  public OpenLockBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Open lock");

    String position = brain.entities.getPosition();
    TreasureData treasure = brain.entities.getTreasures().get(position);

    if (treasure == null) {
      return;
    }

    if (!treasure.isLocked()) {
      return;
    }

    AgentData myself = brain.entities.getMyself();
    if (!myself.canOpenLock(treasure.getLockStrength())) {
      return;
    }

    ((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType());

    this.brain.observe(this.myAgent);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
