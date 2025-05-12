package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.princ.Utils;

public class TryOpenLockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7364592847338788621L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private int counter;

  private Brain brain;

  public TryOpenLockBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  private void initialize() {
    counter = 1000;

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    if (!initialized) {
      initialize();
    }

    brain.mind.setCoordinationPartner(brain.mind.getCoalitionChild());

    if (counter <= 0) {
      resetCoordination();
      initialized = false;
      this.exitValue = 2;
      return;
    }
    counter--;

    this.brain.observe(this.myAgent);
    brain.log(brain.entities.getPosition());
    brain.log(brain.entities.getMyself().getExpertise().get(Observation.LOCKPICKING));

    TreasureData treasure = brain.entities.getTreasures().get(brain.entities.getMyself().getPosition());
    if (treasure == null) {
      return;
    }
    brain.log("LOCKPICKING STRENGTH", treasure.getLockpickStrength());

    brain.log("MY TYPE:", brain.entities.getMyself().getTreasureType());
    boolean success = ((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType());
    brain.log("tried to open, result:", success);
    if (success) {
      treasure = brain.entities.getTreasures().get(brain.entities.getMyself().getPosition());
      treasure.setLocked(false);
      this.brain.observe(this.myAgent);

      resetCoordination();

      initialized = false;
      this.exitValue = 1;
      return;
    }

    Utils.waitFor(myAgent, 400);

    this.exitValue = 0;
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
