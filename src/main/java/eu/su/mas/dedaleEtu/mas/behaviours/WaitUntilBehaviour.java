package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Deque;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.princ.Utils;

public class WaitUntilBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233984986594838272L;

  private boolean initialized = false;
  private int exitValue = 0;

  private Brain brain;
  private List<String> searchingAgents;

  private long waitingTime;
  private long checkTime;

  private static String PROTOCOL_NAME = "Ask to Move";

  public WaitUntilBehaviour(Agent agent, Brain brain, long waitingTime) {
    super(agent);
    this.brain = brain;
    this.waitingTime = waitingTime;
    this.checkTime = 100;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Wait Until");

    this.brain.observe(this.myAgent);
    
    if (this.waitingTime < 1000000000){
      this.waitingTime = this.waitingTime - this.checkTime;
      if (this.waitingTime <= this.checkTime){
        this.exitValue = 1;
      }
      else {
        this.exitValue = 0;
      } 
    }
    
    Utils.waitFor(this.myAgent, this.checkTime);
    brain.entities.ageEntities();
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
