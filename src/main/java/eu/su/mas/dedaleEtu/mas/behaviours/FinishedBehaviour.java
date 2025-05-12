package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class FinishedBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7864532642383945841L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private int counter;

  private Brain brain;

  public FinishedBehaviour(String state, Agent agent, Brain brain) {
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
    brain.log(brain.entities.getPosition());

    if (!this.initialized) {
      initialize();
    }

    this.brain.observe(this.myAgent);

    brain.log(brain.entities.getMyself().getExpertise().get(Observation.LOCKPICKING));
    brain.mind.setCoordinationPartner(brain.mind.getCoalitionChild());

    if (counter <= 0) {
      resetCoordination();
      initialized = false;
      this.exitValue = 1;
      return;
    }
    counter--;

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
