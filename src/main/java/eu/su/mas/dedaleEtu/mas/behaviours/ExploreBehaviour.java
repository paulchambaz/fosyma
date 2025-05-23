package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class ExploreBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374647573871453875L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public ExploreBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.mind.updateBehaviouralPriorities();

    if (brain.mind.isCollectionPreferred()) {
      this.exitValue = 1;
      return;
    }

    brain.observe(this.myAgent);

    if (!brain.map.hasOpenNode()) {
      brain.mind.wantsToTalk();
      brain.log("Finished exploration");
      this.exitValue = 1;
      return;
    }

    String goal = brain.findClosestOpenNode(true);

    if (goal == null) {
      goal = brain.findClosestOpenNode(false);
      return;
    }

    brain.mind.setTargetNode(goal);

  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
