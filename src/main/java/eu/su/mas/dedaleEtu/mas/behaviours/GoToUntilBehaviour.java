package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Deque;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Computes;

public class GoToUntilBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private boolean initialized = false;
  private int exitValue = 0;

  private Brain brain;
  private List<String> searchingAgents;

  public GoToUntilBehaviour(Agent myagent, Brain brain, List<String> searchingAgents) {
    super(myagent);
    this.brain = brain;
    this.searchingAgents = searchingAgents;
  }

  private void initialize() {
    brain.computePathToTarget(true);
    if (this.brain.mind.getPathToTarget().isEmpty()) {
      brain.computePathToTarget(false);
    }

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Go To Until");

    if (!this.initialized) {
      initialize();
    }

    if (brain.mind.isStuck()) {
      this.initialized = false;
      this.exitValue = 2;
      return;
    }

    this.brain.observe(this.myAgent);

    Deque<String> path = this.brain.mind.getPathToTarget();
    if (path.isEmpty()) {
      brain.mind.wantsToTalk();
      this.initialized = false;
      this.exitValue = 1;
      return;
    }

    String position = brain.entities.getPosition();
    String foundAgent = Computes.findSearchedAgentInNeighborhood(brain.map, brain.entities, position,
        this.searchingAgents);
    if (foundAgent != null) {
      this.exitValue = 3;
    }

    String next = path.removeFirst();

    if (brain.moveTo(this.myAgent, next)) {
      brain.mind.decrementStuckCounter();
      brain.mind.incrementSocialCooldown();
      brain.entities.ageEntities();
    } else {
      brain.mind.incrementStuckCounter();
      brain.computePathToTarget(false);
    }

    this.exitValue = 0;
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
