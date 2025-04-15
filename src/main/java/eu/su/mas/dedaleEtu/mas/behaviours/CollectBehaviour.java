package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import java.util.List;
import jade.core.Agent;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

// Main exploration behaviour
public class CollectBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private int exitValue = 0;

  private Knowledge knowledge;

  public CollectBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    this.knowledge.updateDesireExplore();
    // We check here if we still want to collect or go do something else like explore
    if (!this.knowledge.wantsToCollect()) {
      this.exitValue = 2;
      return;
    }

    String goal = this.knowledge.getClosestTreasure(((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId());
    
    if (goal == null) {
      // TODO: start LA RONDE
      System.out.println(this.myAgent.getLocalName() + " wanted to go to a null node");
      this.exitValue = 1;
      return;
    }
    
    this.knowledge.setGoal(goal);
    System.out.println(this.myAgent.getLocalName() + " wants to go to " + goal);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
