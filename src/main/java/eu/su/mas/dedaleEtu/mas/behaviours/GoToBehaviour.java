package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Deque;
import java.util.List;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
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
    brain.computePathToTarget(true);
    if (brain.mind.getPathToTarget() == null || brain.mind.getPathToTarget().isEmpty()) {
      brain.computePathToTarget(false);
    }

    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    Utils.waitFor(this.myAgent, 300);

    brain.mind.setBehaviour("Go To");

    if (!this.initialized) {
      initialize();
    }

    if (brain.mind.isStuck()) {
      if (brain.mind.getAskedMoving() == false){
        brain.mind.askToMove();
        this.exitValue = 3;
      }
      else{
        this.initialized = false;
        this.exitValue = 2;
        brain.mind.resetAskedMoving();
      }
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

    String next = path.removeFirst();

    if (brain.moveTo(this.myAgent, next)) {
      brain.mind.decrementStuckCounter();
      brain.mind.incrementSocialCooldown();
      brain.entities.ageEntities();
    } else {
      brain.mind.incrementStuckCounter();
      brain.computePathToTarget(false);
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
