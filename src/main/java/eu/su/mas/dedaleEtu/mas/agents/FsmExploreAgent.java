package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.InitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToUntilAgentBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExclusiveCommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ComputeEndPositionBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareBrainBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PlanExplorationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SetMeetingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CoordinationNegotiationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.RestoreTargetBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EmptyBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CoordinationLocateAgentLite;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FsmExploreAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -72659168426454387L;

  private Brain brain;

  private static final String INIT = "INIT";

  private static final String EXPLORE = "EXPLORE";
  private static final String EXPLORE_GOTO = "EXPLORE_GOTO";
  private static final String EXPLORE_DEADLOCK = "EXPLORE_DEADLOCK";
  private static final String EXPLORE_COMM = "EXPLORE_COMM";
  private static final String EXPLORE_COMM_SHARE = "EXPLORE_COMM_SHARE";
  private static final String EXPLORE_COMM_MEETING = "EXPLORE_COMM_MEETING";
  private static final String EXPLORE_COMM_PLAN = "EXPLORE_COMM_PLAN";

  private static final String CHEST_NEGOTIATION = "CHEST_NEGOTIATION";

  private static final String MANAGER_LOCATE_AGENT = "MANAGER_LOCATE_AGENT";
  private static final String MANAGER_GOTO_AGENT = "MANAGER_GOTO_AGENT";
  private static final String MANAGER_DEADLOCK = "MANAGER_DEADLOCK";
  private static final String MANAGER_COMM = "MANAGER_COMM";
  private static final String MANAGER_COMM_SHARE = "MANAGER_COMM_SHARE";
  private static final String MANAGER_COMM_MEETING = "MANAGER_COMM_MEETING";

  private static final String FOLLOWER_GOTO = "FOLLOWER_GOTO";
  private static final String FOLLOWER_DEADLOCK = "FOLLOWER_DEADLOCK";
  private static final String FOLLOWER_GOTO_DEADLOCK = "FOLLOWER_GOTO_DEADLOCK";
  private static final String FOLLOWER_RESTORE = "FOLLOWER_RESTORE";
  private static final String FOLLOWER_ARRIVED = "FOLLOWER_ARRIVED";

  private static final String END_LOCATE = "END_LOCATE";
  private static final String END_GOTO = "END_GOTO";
  private static final String END_WAIT = "END_WAIT";
  private static final String END_COMM = "END_COMM";
  private static final String END_COMM_SHARE = "END_COMM_SHARE";
  private static final String END_COMM_MEETING = "END_COMM_MEETING";
  private static final String END_DEADLOCK = "END_DEADLOCK";
  private static final String END_GOTO_DEADLOCK = "END_GOTO_DEADLOCK";

  private static final String END = "END";

  protected void setup() {
    super.setup();

    this.brain = new Brain(this.getLocalName());

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

    // - Behaviours -

    // Init behaviour
    fsmBehaviour.registerFirstState(new InitBehaviour(INIT, this, this.brain), INIT);

    // Explore behaviours
    fsmBehaviour.registerState(new ExploreBehaviour(EXPLORE, this, this.brain), EXPLORE);
    fsmBehaviour.registerState(new GoToBehaviour(EXPLORE_GOTO, this, this.brain), EXPLORE_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(EXPLORE_DEADLOCK, this, this.brain), EXPLORE_DEADLOCK);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(EXPLORE_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("treasure-coordination", 2);
            put("pleasemove", -1);
          }
        }), EXPLORE_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(EXPLORE_COMM_SHARE, this, this.brain), EXPLORE_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(EXPLORE_COMM_MEETING, this, this.brain), EXPLORE_COMM_MEETING);
    fsmBehaviour.registerState(new PlanExplorationBehaviour(EXPLORE_COMM_PLAN, this, this.brain), EXPLORE_COMM_PLAN);

    fsmBehaviour.registerState(new CoordinationNegotiationBehaviour(CHEST_NEGOTIATION, this, this.brain),
        CHEST_NEGOTIATION);

    // Manager behaviours
    fsmBehaviour.registerState(new CoordinationLocateAgentLite(MANAGER_LOCATE_AGENT, this, this.brain),
        MANAGER_LOCATE_AGENT);
    fsmBehaviour.registerState(new GoToUntilAgentBehaviour(MANAGER_GOTO_AGENT, this, this.brain),
        MANAGER_GOTO_AGENT);
    fsmBehaviour.registerState(new DeadlockBehaviour(MANAGER_DEADLOCK, this, this.brain),
        MANAGER_DEADLOCK);
    fsmBehaviour
        .registerState(
            new ExclusiveCommunicationBehaviour(MANAGER_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
              {
                put("treasure-coordination", 1);
                put("sharemap", 2);
                put("pleasemove", -1);
              }
            }), MANAGER_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(MANAGER_COMM_SHARE, this, this.brain), MANAGER_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(MANAGER_COMM_MEETING, this, this.brain), MANAGER_COMM_MEETING);

    // Follower behaviours
    fsmBehaviour.registerState(new GoToBehaviour(FOLLOWER_GOTO, this, this.brain), FOLLOWER_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(FOLLOWER_DEADLOCK, this, this.brain), FOLLOWER_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(FOLLOWER_GOTO_DEADLOCK, this, this.brain), FOLLOWER_GOTO_DEADLOCK);
    fsmBehaviour.registerState(new RestoreTargetBehaviour(FOLLOWER_RESTORE, this, this.brain), FOLLOWER_RESTORE);
    fsmBehaviour.registerState(new EmptyBehaviour(FOLLOWER_ARRIVED, this, this.brain), FOLLOWER_ARRIVED);

    fsmBehaviour.registerState(new ComputeEndPositionBehaviour(END_LOCATE, this, this.brain), END_LOCATE);
    fsmBehaviour.registerState(new GoToBehaviour(END_GOTO, this, this.brain), END_GOTO);
    fsmBehaviour.registerState(new WaitUntilBehaviour(END_WAIT, this, this.brain), END_WAIT);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(END_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("treasure-coordination", 2);
            put("pleasemove", -1);
          }
        }), END_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(END_COMM_SHARE, this, this.brain), END_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(END_COMM_MEETING, this, this.brain), END_COMM_MEETING);
    fsmBehaviour.registerState(new DeadlockBehaviour(END_DEADLOCK, this, this.brain), END_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(END_GOTO_DEADLOCK, this, this.brain), END_GOTO_DEADLOCK);

    // End behaviour
    fsmBehaviour.registerLastState(new EndBehaviour(END, this, this.brain), END);

    // - Transitions -

    // Init transitions
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE);

    // Explore transitions
    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE, END_LOCATE, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_COMM);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(EXPLORE_COMM, CHEST_NEGOTIATION, 2);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_DEADLOCK, -1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_SHARE, EXPLORE_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_MEETING, EXPLORE_COMM_PLAN);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_PLAN, EXPLORE_GOTO);

    fsmBehaviour.registerDefaultTransition(EXPLORE_DEADLOCK, EXPLORE_GOTO);

    fsmBehaviour.registerDefaultTransition(CHEST_NEGOTIATION, EXPLORE);
    fsmBehaviour.registerTransition(CHEST_NEGOTIATION, MANAGER_LOCATE_AGENT, 2);
    fsmBehaviour.registerTransition(CHEST_NEGOTIATION, FOLLOWER_GOTO, 3);

    // Manager transitions
    fsmBehaviour.registerDefaultTransition(MANAGER_LOCATE_AGENT, EXPLORE_DEADLOCK);
    fsmBehaviour.registerTransition(MANAGER_LOCATE_AGENT, MANAGER_GOTO_AGENT, 1);
    fsmBehaviour.registerTransition(MANAGER_LOCATE_AGENT, MANAGER_LOCATE_AGENT, 2);

    fsmBehaviour.registerDefaultTransition(MANAGER_GOTO_AGENT, MANAGER_COMM);
    fsmBehaviour.registerTransition(MANAGER_GOTO_AGENT, MANAGER_LOCATE_AGENT, 1);
    fsmBehaviour.registerTransition(MANAGER_GOTO_AGENT, MANAGER_DEADLOCK, 2);
    fsmBehaviour.registerTransition(MANAGER_GOTO_AGENT, MANAGER_COMM, 3);

    fsmBehaviour.registerDefaultTransition(MANAGER_DEADLOCK, MANAGER_GOTO_AGENT);

    fsmBehaviour.registerDefaultTransition(MANAGER_COMM, MANAGER_GOTO_AGENT);
    fsmBehaviour.registerTransition(MANAGER_COMM, CHEST_NEGOTIATION, 1);
    fsmBehaviour.registerTransition(MANAGER_COMM, MANAGER_COMM_SHARE, 2);
    fsmBehaviour.registerTransition(MANAGER_COMM, MANAGER_DEADLOCK, -1);

    fsmBehaviour.registerDefaultTransition(MANAGER_COMM_SHARE, MANAGER_COMM_MEETING);

    fsmBehaviour.registerDefaultTransition(MANAGER_COMM_MEETING, MANAGER_GOTO_AGENT);

    // Follower transitions
    fsmBehaviour.registerDefaultTransition(FOLLOWER_GOTO, FOLLOWER_GOTO);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO, FOLLOWER_ARRIVED, 1);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO, FOLLOWER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_ARRIVED, FOLLOWER_ARRIVED);
    fsmBehaviour.registerTransition(FOLLOWER_ARRIVED, EXPLORE, 1);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_DEADLOCK, FOLLOWER_GOTO_DEADLOCK);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_GOTO_DEADLOCK);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_RESTORE, 1);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_RESTORE, FOLLOWER_GOTO);

    fsmBehaviour.registerDefaultTransition(END_LOCATE, END_GOTO);

    fsmBehaviour.registerDefaultTransition(END_GOTO, END_GOTO);
    fsmBehaviour.registerTransition(END_GOTO, END_WAIT, 1);
    fsmBehaviour.registerTransition(END_GOTO, END_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(END_WAIT, END_COMM);

    fsmBehaviour.registerDefaultTransition(END_COMM, END_WAIT);
    fsmBehaviour.registerTransition(END_COMM, END_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(END_COMM, CHEST_NEGOTIATION, 2);
    fsmBehaviour.registerTransition(END_COMM, END_DEADLOCK, -1);

    fsmBehaviour.registerDefaultTransition(END_COMM_SHARE, END_COMM_MEETING);

    fsmBehaviour.registerDefaultTransition(END_COMM_MEETING, END_WAIT);

    fsmBehaviour.registerDefaultTransition(END_DEADLOCK, END_GOTO_DEADLOCK);

    fsmBehaviour.registerDefaultTransition(END_GOTO_DEADLOCK, END_GOTO_DEADLOCK);
    fsmBehaviour.registerTransition(END_GOTO_DEADLOCK, END_LOCATE, 1);
    fsmBehaviour.registerTransition(END_GOTO_DEADLOCK, END_DEADLOCK, 2);

    List<Behaviour> behaviours = new ArrayList<Behaviour>();
    behaviours.add(fsmBehaviour);
    addBehaviour(new StartMyBehaviours(this, behaviours));
  }

  protected void takeDown() {
    super.takeDown();
  }

  protected void beforeMove() {
    this.brain.beforeMove();
    super.beforeMove();
  }

  protected void afterMove() {
    super.afterMove();
    this.brain.afterMove();
  }
}
