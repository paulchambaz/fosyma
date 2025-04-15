package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Deque;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

public class GoToBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private boolean initialized = false;
  private int exitValue = 0;

  private Brain brain;

  public GoToBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  private void initialize() {
    brain.computePathToTarget();
    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    if (!this.initialized) {
      initialize();
    }

    this.brain.observe(this.myAgent);

    Utils.waitFor(this.myAgent, 50);

    Deque<String> path = this.brain.mind.getPathToTarget();

    if (path.isEmpty()) {
      this.initialized = false;
      this.exitValue = 1;
      return;
    }

    String next = path.removeFirst();

    if (brain.moveTo(this.myAgent, next)) {
      brain.mind.decrementStuckCounter();
      brain.mind.incrementSocialCooldown();
      brain.entities.ageEntities();
    } else {
      brain.mind.incrementStuckCounter();
      brain.computePathToTarget();
    }

    if (brain.mind.isStuck()) {
      this.initialized = false;
      this.exitValue = 2;
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
