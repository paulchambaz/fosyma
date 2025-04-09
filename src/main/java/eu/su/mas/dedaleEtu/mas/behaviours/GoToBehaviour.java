package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Deque;
import java.util.List;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.princ.Utils;

public class GoToBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private boolean initialized = false;
  private int exitValue = 0;

  private Knowledge knowledge;

  public GoToBehaviour(Agent myagent, Knowledge knowledge) {
    super(myagent);
    this.knowledge = knowledge;
  }

  private void initialize() {
    System.out.println(this.myAgent.getLocalName() + " is being initialized");
    this.knowledge.setGoalPath();
    this.exitValue = 0;
    this.initialized = true;
  }

  @Override
  public void action() {
    if (!this.initialized) {
      initialize();
    }

    this.knowledge.observe(this.myAgent);

    Utils.waitFor(this.myAgent, 50);

    Deque<String> path = this.knowledge.getGoalPath();

    if (path.isEmpty()) {
      this.initialized = false;
      this.exitValue = 1;
      return;
    }

    String next = path.removeFirst();

    try {
      ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(next));
    } catch (Exception e) {
      this.knowledge.bumpRageCounter();
      this.knowledge.setGoalPath();
    }

    this.knowledge.introvertRecovery();
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
