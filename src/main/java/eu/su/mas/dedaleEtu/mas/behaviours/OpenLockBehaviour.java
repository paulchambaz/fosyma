package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class OpenLockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -385943593446598313L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public OpenLockBehaviour(String state, Agent agent, Brain brain) {
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

    brain.log("Trying to open treasure in", position);
    boolean success = ((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType());
    if (success) {
      treasure.setLocked(true);
      this.brain.observe(this.myAgent);

      brain.log("success");
      this.exitValue = 0;
    } else {
      brain.log("failure");

      resetCoordination();
      brain.mind.setCoordinationTreasureNode(position);
      this.exitValue = 1;
    }
  }

  private void resetCoordination() {
    brain.mind.setCoordinationState(CoordinationState.NONE);
    brain.mind.setCoordinationPartner(null);
    brain.mind.setCoordinationTreasureNode(null);
    brain.mind.setMetaTargetNode(null);
    brain.mind.setCoalitionMembers(null);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
