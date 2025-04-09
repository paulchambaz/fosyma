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

  private Knowledge knowledge;
  private int exitValue;

  public CollectBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    this.knowledge.updateDesireExplore();

    if (!this.knowledge.wantsToCollect()) {
      this.exitValue = 2;
      return;
    }

    // Check if we still have room in the backpack
    // TODO : for now we consider we have to go to the silo if we have less than 2 spaces for an item but in
    // the future, we could implement a Desire to go back to the silo
    List<Couple<Observation,Integer>> freeSpaces = ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace();
    for (Couple<Observation,Integer> freeSpace : freeSpaces){
      System.out.println(freeSpace);
      if (freeSpace.getRight() < 2){
        String goal = this.knowledge.getClosestTreasure(((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId());
        if (goal == null) {
          // TODO: start LA RONDE
          System.out.println(this.myAgent.getLocalName() + " wanted to go to a null node");
          this.exitValue = 1;
          return;
        }

        this.knowledge.setGoal(goal);
        System.out.println(this.myAgent.getLocalName() + " wants to go to " + goal);
        
        this.exitValue = 1;
      }
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
