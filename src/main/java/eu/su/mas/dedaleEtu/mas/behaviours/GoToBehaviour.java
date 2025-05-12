package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Deque;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class GoToBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private String state;
  private boolean initialized = false;
  private int exitValue = 0;

  private Brain brain;

  public GoToBehaviour(String state, Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
    this.state = state;
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
    brain.mind.setBehaviour(state);

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

    try {
      if (((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType())) {
        this.brain.observe(this.myAgent);
        ((AbstractDedaleAgent) this.myAgent).pick();
      }

      List<AID> silos = Utils.getSilos(this.myAgent);
      boolean success = false;
      for (AID silo : silos) {
        success = ((AbstractDedaleAgent) this.myAgent).emptyMyBackPack(silo.getLocalName());
        if (success) {
          break;
        }
      }
    } catch (Exception e) {
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
