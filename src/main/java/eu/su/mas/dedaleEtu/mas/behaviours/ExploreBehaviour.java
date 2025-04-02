package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class ExploreBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private int exitValue = 0;

  private Knowledge knowledge;

  public ExploreBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    this.knowledge.observe(this.myAgent);

    String goal = this.knowledge.getClosestOpenNode();

    if (goal == null) {
      System.out.println(this.myAgent.getLocalName() + " wanted to go to a trash node");
      this.exitValue = 1;
      return;
    }

    this.knowledge.setGoal(goal);

    if (!this.knowledge.hasOpenNode() && this.knowledge.getDesireExplore() != 1) {
      System.out.println(this.myAgent.getLocalName() + " finished exploring");
      this.exitValue = 1;
      return;
    }

    // this.knowledge.updateDesireExplore();
    // if (this.knowledge.wantsToCollect()) {
    // this.exitValue = 1;
    // return;
    // }

    System.out.println(this.myAgent.getLocalName() + " wants to go to " + goal);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
