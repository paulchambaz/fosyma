package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class ExploreBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private int exitValue = 0;

  private Brain brain;

  public ExploreBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    this.brain.observe(this.myAgent);

    // if (!this.brain.map.hasOpenNode() &&
    // this.brain.mind.getExplorationPriority() != 1) {
    // System.out.println(this.myAgent.getLocalName() + " finished exploring");
    // this.exitValue = 1;
    // return;
    // }

    // String goal = this.brain.map.getClosestOpenNode();
    //
    // if (goal == null) {
    // // TODO: start LA RONDE
    // System.out.println(this.myAgent.getLocalName() + " wanted to go to a null
    // node");
    // this.exitValue = 1;
    // return;
    // }
    //
    // this.brain.mind.setTargetNode(goal);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
