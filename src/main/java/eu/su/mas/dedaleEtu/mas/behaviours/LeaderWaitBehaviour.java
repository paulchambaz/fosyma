package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class LeaderWaitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -7364592847383945821L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public LeaderWaitBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    String followerName = brain.mind.getCoordinationPartner();
    if (followerName == null) {
      brain.log("No follower assigned");
      this.exitValue = 2;
      return;
    }

    this.brain.observe(this.myAgent);

    String currentPosition = brain.entities.getPosition();
    String followerPosition = brain.entities.getAgents().get(followerName).getPosition();

    if (currentPosition.equals(followerPosition)) {
      brain.log("Follower", followerName, "found at current position");
      this.exitValue = 1;
      return;
    }

    Utils.waitFor(this.myAgent, 400);
    brain.entities.ageEntities();
    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
