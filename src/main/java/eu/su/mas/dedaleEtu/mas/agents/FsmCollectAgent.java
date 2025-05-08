package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.InitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CollectBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToUntilAgentBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ComputeEndPositionBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareBrainBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.OpenLockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PickBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.LocateSiloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DropoffBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PlanExplorationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SetMeetingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CoordinationInitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.TreasureCoordinationNegotiationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.LeaderGuidanceBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.LeaderWaitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.RestoreTargetBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaypointCommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EmptyBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FsmCollectAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -78659868426454587L;

  private Brain brain;

  private static final String INIT = "INIT";

  private static final String EXPLORE = "EXPLORE";
  private static final String EXPLORE_GOTO = "EXPLORE_GOTO";
  private static final String EXPLORE_DEADLOCK = "EXPLORE_DEADLOCK";
  private static final String EXPLORE_COMM = "EXPLORE_COMM";
  private static final String EXPLORE_COMM_SHARE = "EXPLORE_COMM_SHARE";
  private static final String EXPLORE_COMM_MEETING = "EXPLORE_COMM_MEETING";
  private static final String EXPLORE_COMM_PLAN = "EXPLORE_COMM_PLAN";

  private static final String COLLECT = "COLLECT";
  private static final String COLLECT_GOTO = "COLLECT_GOTO";
  private static final String COLLECT_DEADLOCK = "COLLECT_DEADLOCK";
  private static final String COLLECT_COMM = "COLLECT_COMM";
  private static final String COLLECT_COMM_SHARE = "COLLECT_COMM_SHARE";
  private static final String COLLECT_COMM_MEETING = "COLLECT_COMM_MEETING";
  private static final String COLLECT_OPENLOCK = "COLLECT_OPENLOCK";
  private static final String COLLECT_PICK = "COLLECT_PICK";

  private static final String DROP_LOCATE_SILO = "DROP_LOCATE_SILO";
  private static final String DROP_GOTO_SILO = "DROP_GOTO_SILO";
  private static final String DROP_DEADLOCK = "DROP_DEADLOCK";
  private static final String DROP_COMM = "DROP_COMM";
  private static final String DROP_COMM_SHARE = "DROP_COMM_SHARE";
  private static final String DROP_COMM_MEETING = "DROP_COMM_MEETING";
  private static final String DROP_DROPOFF = "DROP_DROPOFF";

  private static final String CHEST_LOCATE_AGENT = "CHEST_LOCATE_AGENT";
  private static final String CHEST_GOTO_AGENT = "CHEST_GOTO_AGENT";
  private static final String CHEST_DEADLOCK = "CHEST_DEADLOCK";
  private static final String CHEST_COMM = "CHEST_COMM";
  private static final String CHEST_COMM_SHARE = "CHEST_COMM_SHARE";
  private static final String CHEST_COMM_MEETING = "CHEST_COMM_MEETING";
  private static final String CHEST_INIT_COMM = "CHEST_INIT_COMM";
  private static final String CHEST_NEGOTIATION = "CHEST_NEGOTIATION";

  private static final String LEADER_COMPUTE_WAYPOINT = "LEADER_COMPUTE_WAYPOINT";
  private static final String LEADER_COMM = "LEADER_COMM";
  private static final String LEADER_COMM_WAYPOINT = "LEADER_COMM_WAYPOINT";
  private static final String LEADER_GOTO = "LEADER_GOTO";
  private static final String LEADER_DEADLOCK = "LEADER_DEADLOCK";
  private static final String LEADER_GOTO_DEADLOCK = "LEADER_GOTO_DEADLOCK";
  private static final String LEADER_RESTORE = "LEADER_RESTORE";
  private static final String LEADER_WAITFOR = "LEADER_WAITFOR";
  private static final String LEADER_COMM_ARRIVED = "LEADER_COMM_CHEST";
  private static final String LEADER_OPENLOCK = "LEADER_OPENLOCK";

  private static final String FOLLOWER = "FOLLOWER";
  private static final String FOLLOWER_COMM = "FOLLOWER_COMM";
  private static final String FOLLOWER_COMM_WAYPOINT = "FOLLOWER_COMM_WAYPOINT";
  private static final String FOLLOWER_GOTO = "FOLLOWER_GOTO";
  private static final String FOLLOWER_DEADLOCK = "FOLLOWER_DEADLOCK";
  private static final String FOLLOWER_GOTO_DEADLOCK = "FOLLOWER_GOTO_DEADLOCK";
  private static final String FOLLOWER_RESTORE = "FOLLOWER_RESTORE";
  private static final String FOLLOWER_OPENLOCK = "FOLLOWER_OPENLOCK";

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
            put("pleasemove", 3);
          }
        }), EXPLORE_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(EXPLORE_COMM_SHARE, this, this.brain), EXPLORE_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(EXPLORE_COMM_MEETING, this, this.brain), EXPLORE_COMM_MEETING);
    fsmBehaviour.registerState(new PlanExplorationBehaviour(EXPLORE_COMM_PLAN, this, this.brain), EXPLORE_COMM_PLAN);

    // Collect behaviours
    fsmBehaviour.registerState(new CollectBehaviour(COLLECT, this, this.brain), COLLECT);
    fsmBehaviour.registerState(new GoToBehaviour(COLLECT_GOTO, this, this.brain), COLLECT_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(COLLECT_DEADLOCK, this, this.brain), COLLECT_DEADLOCK);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(COLLECT_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("treasure-coordination", 2);
            put("pleasemove", 3);
          }
        }), COLLECT_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(COLLECT_COMM_SHARE, this, this.brain), COLLECT_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(COLLECT_COMM_MEETING, this, this.brain), COLLECT_COMM_MEETING);
    fsmBehaviour.registerState(new OpenLockBehaviour(COLLECT_OPENLOCK, this, this.brain), COLLECT_OPENLOCK);
    fsmBehaviour.registerState(new PickBehaviour(COLLECT_PICK, this, this.brain), COLLECT_PICK);

    // Drop behaviours
    fsmBehaviour.registerState(new LocateSiloBehaviour(DROP_LOCATE_SILO, this, this.brain), DROP_LOCATE_SILO);
    fsmBehaviour.registerState(
        new GoToUntilBehaviour(DROP_GOTO_SILO, this, this.brain, new ArrayList<>(Arrays.asList("Silo"))),
        DROP_GOTO_SILO);
    fsmBehaviour.registerState(new DeadlockBehaviour(DROP_DEADLOCK, this, this.brain), DROP_DEADLOCK);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(DROP_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("pleasemove", 2);
          }
        }), DROP_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(DROP_COMM_SHARE, this, this.brain), DROP_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(DROP_COMM_MEETING, this, this.brain), DROP_COMM_MEETING);
    fsmBehaviour.registerState(new DropoffBehaviour(DROP_DROPOFF, this, this.brain), DROP_DROPOFF);

    // Chest behaviours
    fsmBehaviour.registerState(new CoordinationInitBehaviour(CHEST_LOCATE_AGENT, this, this.brain), CHEST_LOCATE_AGENT);
    fsmBehaviour.registerState(new GoToUntilAgentBehaviour(CHEST_GOTO_AGENT, this, this.brain), CHEST_GOTO_AGENT);
    fsmBehaviour.registerState(new DeadlockBehaviour(CHEST_DEADLOCK, this, this.brain), CHEST_DEADLOCK);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(CHEST_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("treasure-coordination", 2);
            put("pleasemove", 3);
          }
        }), CHEST_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(CHEST_COMM_SHARE, this, this.brain), CHEST_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(CHEST_COMM_MEETING, this, this.brain), CHEST_COMM_MEETING);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(CHEST_INIT_COMM, this, this.brain, 2, new HashMap<String, Integer>() {
          {
            put("treasure-coordination", 1);
            put("pleasemove", 2);
          }
        }), CHEST_INIT_COMM);
    fsmBehaviour.registerState(new TreasureCoordinationNegotiationBehaviour(CHEST_NEGOTIATION, this, this.brain),
        CHEST_NEGOTIATION);

    // Leader behaviours
    fsmBehaviour.registerState(new LeaderGuidanceBehaviour(LEADER_COMPUTE_WAYPOINT, this, this.brain),
        LEADER_COMPUTE_WAYPOINT);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(LEADER_COMM, this, this.brain, 3, new HashMap<String, Integer>() {
          {
            put("waypoint-guidance", 1);
            put("pleasemove", 2);
          }
        }), LEADER_COMM);
    fsmBehaviour.registerState(new WaypointCommunicationBehaviour(LEADER_COMM_WAYPOINT, this, this.brain),
        LEADER_COMM_WAYPOINT);
    fsmBehaviour.registerState(new GoToBehaviour(LEADER_GOTO, this, this.brain), LEADER_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(LEADER_DEADLOCK, this, this.brain), LEADER_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(LEADER_GOTO_DEADLOCK, this, this.brain), LEADER_GOTO_DEADLOCK);
    fsmBehaviour.registerState(new RestoreTargetBehaviour(LEADER_RESTORE, this, this.brain), LEADER_RESTORE);
    fsmBehaviour.registerState(new LeaderWaitBehaviour(LEADER_WAITFOR, this, this.brain), LEADER_WAITFOR);
    fsmBehaviour.registerState(
        new CommunicationBehaviour(LEADER_COMM_ARRIVED, this, this.brain, 4, new HashMap<String, Integer>() {
          {
            put("arrived", 1);
            put("pleasemove", 2);
          }
        }), LEADER_COMM_ARRIVED);
    fsmBehaviour.registerState(new OpenLockBehaviour(LEADER_OPENLOCK, this, this.brain), LEADER_OPENLOCK);

    // Follower behaviours
    fsmBehaviour.registerState(new EmptyBehaviour(FOLLOWER, this, this.brain), FOLLOWER);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(FOLLOWER_COMM, this, this.brain, 3, new HashMap<String, Integer>() {
          {
            put("waypoint-guidance", 1);
            put("arrived", 2);
            put("pleasemove", 3);
          }
        }), FOLLOWER_COMM);
    fsmBehaviour.registerState(new WaypointCommunicationBehaviour(FOLLOWER_COMM_WAYPOINT, this, this.brain),
        FOLLOWER_COMM_WAYPOINT);
    fsmBehaviour.registerState(new GoToBehaviour(FOLLOWER_GOTO, this, this.brain), FOLLOWER_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(FOLLOWER_DEADLOCK, this, this.brain), FOLLOWER_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(FOLLOWER_GOTO_DEADLOCK, this, this.brain), FOLLOWER_GOTO_DEADLOCK);
    fsmBehaviour.registerState(new RestoreTargetBehaviour(FOLLOWER_RESTORE, this, this.brain), FOLLOWER_RESTORE);
    fsmBehaviour.registerState(new OpenLockBehaviour(FOLLOWER_OPENLOCK, this, this.brain), FOLLOWER_OPENLOCK);

    fsmBehaviour.registerState(new ComputeEndPositionBehaviour(END_LOCATE, this, this.brain), END_LOCATE);
    fsmBehaviour.registerState(new GoToBehaviour(END_GOTO, this, this.brain), END_GOTO);
    fsmBehaviour.registerState(new WaitUntilBehaviour(END_WAIT, this, this.brain), END_WAIT);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(END_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("pleasemove", 2);
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
    fsmBehaviour.registerTransition(EXPLORE, COLLECT, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_COMM);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(EXPLORE_COMM, CHEST_NEGOTIATION, 2);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_DEADLOCK, 3);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_SHARE, EXPLORE_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_MEETING, EXPLORE_COMM_PLAN);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_PLAN, EXPLORE_GOTO);

    fsmBehaviour.registerDefaultTransition(EXPLORE_DEADLOCK, EXPLORE_GOTO);

    // Collect transitions
    fsmBehaviour.registerDefaultTransition(COLLECT, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT, END_LOCATE, 1);
    fsmBehaviour.registerTransition(COLLECT, EXPLORE, 2);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO, COLLECT_COMM);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_OPENLOCK, 1);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(COLLECT_COMM, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT_COMM, COLLECT_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(COLLECT_COMM, CHEST_NEGOTIATION, 2);
    fsmBehaviour.registerTransition(COLLECT_COMM, COLLECT_DEADLOCK, 3);

    fsmBehaviour.registerDefaultTransition(COLLECT_COMM_SHARE, COLLECT_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(COLLECT_COMM_MEETING, COLLECT_GOTO);

    fsmBehaviour.registerDefaultTransition(COLLECT_DEADLOCK, COLLECT_GOTO);

    fsmBehaviour.registerDefaultTransition(COLLECT_OPENLOCK, COLLECT_PICK);
    fsmBehaviour.registerTransition(COLLECT_OPENLOCK, CHEST_LOCATE_AGENT, 1);

    fsmBehaviour.registerDefaultTransition(COLLECT_PICK, DROP_LOCATE_SILO);

    // Drop transitions
    fsmBehaviour.registerDefaultTransition(DROP_LOCATE_SILO, DROP_GOTO_SILO);

    fsmBehaviour.registerDefaultTransition(DROP_GOTO_SILO, DROP_COMM);
    fsmBehaviour.registerTransition(DROP_GOTO_SILO, DROP_LOCATE_SILO, 1);
    fsmBehaviour.registerTransition(DROP_GOTO_SILO, DROP_DEADLOCK, 2);
    fsmBehaviour.registerTransition(DROP_GOTO_SILO, DROP_DROPOFF, 3);

    fsmBehaviour.registerDefaultTransition(DROP_COMM, DROP_GOTO_SILO);
    fsmBehaviour.registerTransition(DROP_COMM, DROP_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(DROP_COMM, DROP_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(DROP_COMM_SHARE, DROP_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(DROP_COMM_MEETING, DROP_GOTO_SILO);

    fsmBehaviour.registerDefaultTransition(DROP_DEADLOCK, DROP_GOTO_SILO);

    fsmBehaviour.registerDefaultTransition(DROP_DROPOFF, COLLECT);
    fsmBehaviour.registerTransition(DROP_DROPOFF, DROP_LOCATE_SILO, 1);

    // Chest coordination transitions
    fsmBehaviour.registerDefaultTransition(CHEST_LOCATE_AGENT, COLLECT);
    fsmBehaviour.registerTransition(CHEST_LOCATE_AGENT, CHEST_GOTO_AGENT, 1);
    fsmBehaviour.registerTransition(CHEST_LOCATE_AGENT, DROP_LOCATE_SILO, 2);

    fsmBehaviour.registerDefaultTransition(CHEST_GOTO_AGENT, CHEST_COMM);
    fsmBehaviour.registerTransition(CHEST_GOTO_AGENT, CHEST_LOCATE_AGENT, 1);
    fsmBehaviour.registerTransition(CHEST_GOTO_AGENT, CHEST_DEADLOCK, 2);
    fsmBehaviour.registerTransition(CHEST_GOTO_AGENT, CHEST_DEADLOCK, 3);

    fsmBehaviour.registerDefaultTransition(CHEST_INIT_COMM, CHEST_LOCATE_AGENT);
    fsmBehaviour.registerTransition(CHEST_INIT_COMM, CHEST_NEGOTIATION, 1);
    fsmBehaviour.registerTransition(CHEST_INIT_COMM, CHEST_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(CHEST_DEADLOCK, CHEST_GOTO_AGENT);

    fsmBehaviour.registerDefaultTransition(CHEST_COMM, CHEST_GOTO_AGENT);
    fsmBehaviour.registerTransition(CHEST_COMM, CHEST_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(CHEST_COMM, CHEST_NEGOTIATION, 2);
    fsmBehaviour.registerTransition(CHEST_COMM, CHEST_DEADLOCK, 3);

    fsmBehaviour.registerDefaultTransition(CHEST_COMM_SHARE, CHEST_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(CHEST_COMM_MEETING, CHEST_NEGOTIATION);

    fsmBehaviour.registerDefaultTransition(CHEST_NEGOTIATION, COLLECT);
    fsmBehaviour.registerTransition(CHEST_NEGOTIATION, LEADER_COMPUTE_WAYPOINT, 1);
    fsmBehaviour.registerTransition(CHEST_NEGOTIATION, FOLLOWER, 2);

    // Leader transitions
    fsmBehaviour.registerDefaultTransition(LEADER_COMPUTE_WAYPOINT, LEADER_COMM);

    fsmBehaviour.registerDefaultTransition(LEADER_COMM, CHEST_LOCATE_AGENT);
    fsmBehaviour.registerTransition(LEADER_COMM, LEADER_COMM_WAYPOINT, 1);
    fsmBehaviour.registerTransition(LEADER_COMM, LEADER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(LEADER_COMM_WAYPOINT, LEADER_GOTO);

    fsmBehaviour.registerDefaultTransition(LEADER_GOTO, LEADER_GOTO);
    fsmBehaviour.registerTransition(LEADER_GOTO, LEADER_WAITFOR, 1);
    fsmBehaviour.registerTransition(LEADER_GOTO, LEADER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(LEADER_DEADLOCK, LEADER_GOTO_DEADLOCK);

    fsmBehaviour.registerDefaultTransition(LEADER_GOTO_DEADLOCK, LEADER_GOTO_DEADLOCK);
    fsmBehaviour.registerTransition(LEADER_GOTO_DEADLOCK, LEADER_RESTORE, 1);
    fsmBehaviour.registerTransition(LEADER_GOTO_DEADLOCK, LEADER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(LEADER_RESTORE, LEADER_GOTO);

    fsmBehaviour.registerDefaultTransition(LEADER_WAITFOR, LEADER_WAITFOR);
    fsmBehaviour.registerTransition(LEADER_WAITFOR, LEADER_COMM_ARRIVED, 1);

    fsmBehaviour.registerDefaultTransition(LEADER_COMM_ARRIVED, CHEST_LOCATE_AGENT);
    fsmBehaviour.registerTransition(LEADER_COMM_ARRIVED, LEADER_OPENLOCK, 1);

    fsmBehaviour.registerDefaultTransition(LEADER_OPENLOCK, COLLECT_PICK);

    // Follower transitions
    fsmBehaviour.registerDefaultTransition(FOLLOWER, FOLLOWER_COMM);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_COMM, FOLLOWER);
    fsmBehaviour.registerTransition(FOLLOWER_COMM, FOLLOWER_COMM_WAYPOINT, 1);
    fsmBehaviour.registerTransition(FOLLOWER_COMM, FOLLOWER_OPENLOCK, 2);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_COMM_WAYPOINT, FOLLOWER_GOTO);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_GOTO, FOLLOWER_GOTO);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO, FOLLOWER_COMM, 1);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO, FOLLOWER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_DEADLOCK, FOLLOWER_GOTO_DEADLOCK);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_GOTO_DEADLOCK);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_RESTORE, 1);
    fsmBehaviour.registerTransition(FOLLOWER_GOTO_DEADLOCK, FOLLOWER_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_RESTORE, FOLLOWER_GOTO);

    fsmBehaviour.registerDefaultTransition(FOLLOWER_OPENLOCK, COLLECT);

    fsmBehaviour.registerDefaultTransition(END_LOCATE, END_GOTO);

    fsmBehaviour.registerDefaultTransition(END_GOTO, END_GOTO);
    fsmBehaviour.registerTransition(END_GOTO, END_WAIT, 1);
    fsmBehaviour.registerTransition(END_GOTO, END_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(END_WAIT, END_COMM);

    fsmBehaviour.registerDefaultTransition(END_COMM, END_WAIT);
    fsmBehaviour.registerTransition(END_COMM, END_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(END_COMM, END_DEADLOCK, 2);

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
