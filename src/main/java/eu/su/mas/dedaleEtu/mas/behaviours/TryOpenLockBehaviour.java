package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
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
    counter = 10000;

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    if (!initialized) {
      initialize();
    }

    this.brain.observe(this.myAgent);
    brain.log(brain.entities.getPosition());
    brain.log(brain.entities.getMyself().getExpertise().get(Observation.LOCKPICKING));

    TreasureData treasure = brain.entities.getTreasures().get(brain.entities.getMyself().getPosition());
    brain.log("LOCKPICKING STRENGTH", treasure.getLockpickStrength());

    if (counter <= 0) {
      initialized = false;
      this.exitValue = 2;
      return;
    }
    counter--;

    brain.log("MY TYPE:", brain.entities.getMyself().getTreasureType());
    boolean success = ((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType());
    brain.log("tried to open, result:", success);
    if (success) {
      treasure = brain.entities.getTreasures().get(brain.entities.getMyself().getPosition());
      treasure.setLocked(true);
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
