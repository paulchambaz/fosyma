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

  private static final String END = "END";

  protected void setup() {
    super.setup();

    this.brain = new Brain(this.getLocalName());

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

    // - Behaviours -

    // Init behaviour
    fsmBehaviour.registerFirstState(new InitBehaviour(this, this.brain), INIT);

    // Explore behaviours
    fsmBehaviour.registerState(new ExploreBehaviour(this, this.brain), EXPLORE);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), EXPLORE_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), EXPLORE_DEADLOCK);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 1, new HashMap<String, Integer>() {
      {
        put("sharemap", 1);
        put("treasure-coordination", 2);
        put("pleasemove", 3);
      }
    }), EXPLORE_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), EXPLORE_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(this, this.brain), EXPLORE_COMM_MEETING);
    fsmBehaviour.registerState(new PlanExplorationBehaviour(this, this.brain), EXPLORE_COMM_PLAN);

    // Collect behaviours
    fsmBehaviour.registerState(new CollectBehaviour(this, this.brain), COLLECT);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), COLLECT_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), COLLECT_DEADLOCK);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 1, new HashMap<String, Integer>() {
      {
        put("sharemap", 1);
        put("treasure-coordination", 2);
        put("pleasemove", 3);
      }
    }), COLLECT_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), COLLECT_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(this, this.brain), COLLECT_COMM_MEETING);
    fsmBehaviour.registerState(new OpenLockBehaviour(this, this.brain), COLLECT_OPENLOCK);
    fsmBehaviour.registerState(new PickBehaviour(this, this.brain), COLLECT_PICK);

    // Drop behaviours
    fsmBehaviour.registerState(new LocateSiloBehaviour(this, this.brain), DROP_LOCATE_SILO);
    fsmBehaviour.registerState(new GoToUntilBehaviour(this, this.brain, new ArrayList<>(Arrays.asList("Silo"))),
        DROP_GOTO_SILO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), DROP_DEADLOCK);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 1, new HashMap<String, Integer>() {
      {
        put("sharemap", 1);
        put("pleasemove", 2);
      }
    }), DROP_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), DROP_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(this, this.brain), DROP_COMM_MEETING);
    fsmBehaviour.registerState(new DropoffBehaviour(this, this.brain), DROP_DROPOFF);

    // Chest behaviours
    fsmBehaviour.registerState(new CoordinationInitBehaviour(this, this.brain), CHEST_LOCATE_AGENT);
    fsmBehaviour.registerState(new GoToUntilAgentBehaviour(this, this.brain), CHEST_GOTO_AGENT);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), CHEST_DEADLOCK);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 1, new HashMap<String, Integer>() {
      {
        put("sharemap", 1);
        put("treasure-coordination", 2);
        put("pleasemove", 3);
      }
    }), CHEST_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), CHEST_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(this, this.brain), CHEST_COMM_MEETING);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 2, new HashMap<String, Integer>() {
      {
        put("treasure-coordination", 1);
        put("pleasemove", 2);
      }
    }), CHEST_INIT_COMM);
    fsmBehaviour.registerState(new TreasureCoordinationNegotiationBehaviour(this, this.brain), CHEST_NEGOTIATION);

    // Leader behaviours
    fsmBehaviour.registerState(new LeaderGuidanceBehaviour(this, this.brain), LEADER_COMPUTE_WAYPOINT);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 3, new HashMap<String, Integer>() {
      {
        put("waypoint-guidance", 1);
        put("pleasemove", 2);
      }
    }), LEADER_COMM);
    fsmBehaviour.registerState(new WaypointCommunicationBehaviour(this, this.brain), LEADER_COMM_WAYPOINT);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), LEADER_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), LEADER_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), LEADER_GOTO_DEADLOCK);
    fsmBehaviour.registerState(new RestoreTargetBehaviour(this, this.brain), LEADER_RESTORE);
    fsmBehaviour.registerState(new LeaderWaitBehaviour(this, this.brain), LEADER_WAITFOR);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 4, new HashMap<String, Integer>() {
      {
        put("arrived", 1);
        put("pleasemove", 2);
      }
    }), LEADER_COMM_ARRIVED);
    fsmBehaviour.registerState(new OpenLockBehaviour(this, this.brain), LEADER_OPENLOCK);

    // Follower behaviours
    fsmBehaviour.registerState(new EmptyBehaviour(this, this.brain), FOLLOWER);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, 3, new HashMap<String, Integer>() {
      {
        put("waypoint-guidance", 1);
        put("arrived", 2);
        put("pleasemove", 3);
      }
    }), FOLLOWER_COMM);
    fsmBehaviour.registerState(new WaypointCommunicationBehaviour(this, this.brain), FOLLOWER_COMM_WAYPOINT);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), FOLLOWER_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), FOLLOWER_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), FOLLOWER_GOTO_DEADLOCK);
    fsmBehaviour.registerState(new RestoreTargetBehaviour(this, this.brain), FOLLOWER_RESTORE);
    fsmBehaviour.registerState(new OpenLockBehaviour(this, this.brain), FOLLOWER_OPENLOCK);

    // End behaviour
    fsmBehaviour.registerLastState(new EndBehaviour(this, this.brain), END);

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
    fsmBehaviour.registerTransition(COLLECT, END, 1);
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
