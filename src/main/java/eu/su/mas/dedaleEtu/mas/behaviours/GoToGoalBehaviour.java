package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import jade.core.behaviours.OneShotBehaviour;
import java.util.Deque;
import jade.core.Agent;

public class GoToGoalBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 1233959882640838272L;

  private boolean initialized = false;
  private int exitValue;

  private Brain brain;
  private Deque<String> pathToGoal;

  public GoToGoalBehaviour(Agent myagent, Brain brain) {
    super(myagent);
    this.brain = brain;
  }

  // private void initialize() {
  // System.out.println("GOING TO GOAL " + myAgent.getLocalName());
  // Location myPosition = ((AbstractDedaleAgent)
  // this.myAgent).getCurrentPosition();
  // if (myPosition == null) {
  // return;
  // }
  // this.brain.updateAgentPosition(myPosition.getLocationId());
  // // this.brain.getMind().updateGoal(myPosition.getLocationId());
  //
  // this.pathToGoal = this.brain.mind.getPathToTarget();
  // System.out.println(this.pathToGoal);
  // this.initialized = true;
  // }
  //
  @Override
  public void action() {
    // if (!initialized) {
    // initialize();
    // }
    // Location myPosition = ((AbstractDedaleAgent)
    // this.myAgent).getCurrentPosition();
    // if (myPosition == null) {
    // return;
    // }
    // this.brain.updateAgentPosition(myPosition.getLocationId());
    //
    // try {
    // this.myAgent.doWait(500);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // if (pathToGoal.isEmpty()) {
    // // here we normally found the goal, we can check if it's still here in case
    // the
    // // golem moved it and open it
    // List<Couple<Location, List<Couple<Observation, String>>>> observations =
    // ((AbstractDedaleAgent) this.myAgent)
    // .observe();
    // Observation myTreasureType = ((AbstractDedaleAgent)
    // this.myAgent).getMyTreasureType();
    //
    // for (Couple<Location, List<Couple<Observation, String>>> entry :
    // observations) {
    // for (Couple<Observation, String> observation : entry.getRight()) {
    // Observation observeKind = observation.getLeft();
    // String observed = observation.getRight();
    //
    // switch (observeKind) {
    // case GOLD:
    // this.exitValue = 1;
    // break;
    // case AGENTNAME:
    // if (observed.startsWith("Silo"))
    // this.exitValue = 2;
    // break;
    // default:
    // assert false : "Unhandled observation type: " + observeKind;
    // }
    // }
    // }
    // }
    //
    // try {
    // ((AbstractDedaleAgent) this.myAgent).moveTo(new
    // GsLocation(pathToGoal.removeFirst()));
    // } catch (Exception e) {
    // this.brain.mind.incrementStuckCounter();
    // // we are stuck at a point, we can try and recalculate a dijkstra here to the
    // // treasure
    // myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
    // if (myPosition == null) {
    // return;
    // }
    // this.brain.updateAgentPosition(myPosition.getLocationId());
    // // this.brain.updateGoal(myPosition.getLocationId());
    // }
  }

  @Override
  public int onEnd() {
    this.initialized = false;
    return this.exitValue;
  }
}
