package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Computes;

public class MeetingBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573071453865L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public MeetingBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    brain.mind.updateBehaviouralPriorities();

    brain.observe(this.myAgent);

    String goal = brain.entities.getMyself().getMeetingPoint();
    if (goal == null) {
      String currentPosition = brain.entities.getPosition();
      goal = Computes.computeMyMeetingPoint(brain.map, 2.0, 1.5, currentPosition);
    }

    brain.log("going to meeting point in", goal);
    brain.mind.setTargetNode(goal);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
