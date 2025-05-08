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
import eu.su.mas.dedaleEtu.mas.behaviours.DeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareBrainBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.OpenLockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PickBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.LocateSiloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DropoffBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PlanExplorationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.MeetingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SetMeetingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.AskToMoveBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.MoveAsideBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FsmCollectAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -78659868426454587L;

  private Brain brain;

  private static final String INIT = "Init";

  private static final String EXPLORE = "Explore";
  private static final String EXPLORE_GOTO = "Explore go to";
  private static final String EXPLORE_DEADLOCK = "Explore deadlock";
  private static final String EXPLORE_COMMUNICATION = "Explore communication";

  private static final String COLLECT = "Collect";
  private static final String COLLECT_GOTO = "Collect go to";
  private static final String COLLECT_DEADLOCK = "Collect deadlock";
  private static final String COLLECT_OPENLOCK = "Collect open lock";
  private static final String COLLECT_PICK = "Collect Pick";
  private static final String COLLECT_LOCATE_SILO = "Collect Locate Silo";
  private static final String COLLECT_GOTO_UNTIL_SILO = "Collect go to until silo";
  private static final String COLLECT_DEADLOCK_SILO = "Collect deadlock silo";
  private static final String COLLECT_DROPOFF = "Collect dropoff";

  private static final String COMMUNICATION_SHAREMAP = "Communication share map";
  private static final String COMMUNICATION_SOLVEDEADLOCK = "Communication solve deadlock";
  private static final String COMMUNICATION_PLANEXPLORATION = "Communication plan exploration";
  private static final String COMMUNICATION_SETMEETINGPOINT = "Communication set meeting point";

  private static final String MEETING = "Meeting";
  private static final String MEETING_GOTO = "Meeting go to";
  private static final String MEETING_DEADLOCK = "Meeting deadlock";

  private static final String WAIT_UNTIL_MEETING = "Wait until";
  private static final String WAIT_COMMUNICATION = "Wait communication";

  private static final String ASKMOVE_COMMUNICATION = "Ask Move communication";

  private static final String ASKED_MOVE = "Asked Move";
  private static final String ASK_MOVE = "Ask Move";

  private static final String MOVEASIDE = "Move Aside";
  private static final String MOVEASIDE_GOTO = "Move Aside go to";
  private static final String MOVEASIDE_DEADLOCK = "Move Aside deadlock";
  private static final String MOVEASIDE_COMMUNICATION = "Move Aside communication";

  private static final String END = "End";

  protected void setup() {
    super.setup();

    this.brain = new Brain(this.getLocalName());

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

    // behaviours

    // init behaviour
    fsmBehaviour.registerFirstState(new InitBehaviour(this, this.brain), INIT);

    // exploration behaviours
    fsmBehaviour.registerState(new ExploreBehaviour(this, this.brain), EXPLORE);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), EXPLORE_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), EXPLORE_DEADLOCK);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain,
        new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("solvedeadlock", 2);
            put("planexploration", 3);
            put("setmeetingpoint", 4);
          }
        }),
        EXPLORE_COMMUNICATION);

    // collect behaviours
    fsmBehaviour.registerState(new CollectBehaviour(this, this.brain), COLLECT);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), COLLECT_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), COLLECT_DEADLOCK);
    fsmBehaviour.registerState(new OpenLockBehaviour(this, this.brain), COLLECT_OPENLOCK);
    fsmBehaviour.registerState(new PickBehaviour(this, this.brain), COLLECT_PICK);
    fsmBehaviour.registerState(new LocateSiloBehaviour(this, this.brain), COLLECT_PICK);
    fsmBehaviour.registerState(new LocateSiloBehaviour(this, this.brain), COLLECT_LOCATE_SILO);
    fsmBehaviour.registerState(new GoToUntilBehaviour(this, this.brain, new ArrayList<>(Arrays.asList("Silo"))),
        COLLECT_GOTO_UNTIL_SILO);
    fsmBehaviour.registerState(new DropoffBehaviour(this, this.brain), COLLECT_DROPOFF);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), COLLECT_DEADLOCK_SILO);

    // communication behaviours
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), COMMUNICATION_SHAREMAP);
    fsmBehaviour.registerState(new SetMeetingBehaviour(this, this.brain), COMMUNICATION_SETMEETINGPOINT);
    fsmBehaviour.registerState(new PlanExplorationBehaviour(this, this.brain), COMMUNICATION_PLANEXPLORATION);

    // meeting point behaviours
    fsmBehaviour.registerState(new MeetingBehaviour(this, this.brain), MEETING);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), MEETING_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), MEETING_DEADLOCK);

    // waiting behaviours
    fsmBehaviour.registerState(new WaitUntilBehaviour(this, this.brain, 3000), WAIT_UNTIL_MEETING);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, new HashMap<String, Integer>() {
      {
        put("followme", 1);
        put("move aside", 2);
      }
    }),
        WAIT_COMMUNICATION);
    
    fsmBehaviour.registerState(new AskToMoveBehaviour(this, this.brain), ASKED_MOVE);
    
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, new HashMap<String, Integer>() {
      {
        put("move aside", 1);
      }
    }),
        ASKMOVE_COMMUNICATION);
    
    fsmBehaviour.registerState(new AskToMoveBehaviour(this, this.brain), ASK_MOVE);
    
    // move aside behaviours
    fsmBehaviour.registerState(new MoveAsideBehaviour(this, this.brain), MOVEASIDE);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), MOVEASIDE_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), MOVEASIDE_DEADLOCK);

    // end behaviours
    fsmBehaviour.registerLastState(new EndBehaviour(this, this.brain), END);

    // transitions

    // init transitions
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE);

    // explore transitions
    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE, MEETING, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_COMMUNICATION);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMMUNICATION, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_COMMUNICATION, COMMUNICATION_SHAREMAP, 1);
    // fsmBehaviour.registerTransition(EXPLORE_COMMUNICATION, SOLVEDEADLOCK, 2);
    // fsmBehaviour.registerTransition(EXPLORE_COMMUNICATION, PLANEXPLORE, 3);
    fsmBehaviour.registerTransition(EXPLORE_COMMUNICATION, COMMUNICATION_SETMEETINGPOINT, 4);

    fsmBehaviour.registerDefaultTransition(EXPLORE_DEADLOCK, EXPLORE_GOTO);

    // collect transitions
    fsmBehaviour.registerDefaultTransition(COLLECT, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT, END, 1);
    fsmBehaviour.registerTransition(COLLECT, EXPLORE, 2);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_OPENLOCK, 1);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_DEADLOCK, 2);
    fsmBehaviour.registerTransition(COLLECT_GOTO, ASKMOVE_COMMUNICATION, 3);

    fsmBehaviour.registerTransition(ASKMOVE_COMMUNICATION, ASK_MOVE, 1);

    fsmBehaviour.registerDefaultTransition(ASK_MOVE, COLLECT_GOTO);

    fsmBehaviour.registerDefaultTransition(COLLECT_OPENLOCK, COLLECT_PICK);

    fsmBehaviour.registerDefaultTransition(COLLECT_PICK, COLLECT_LOCATE_SILO);

    fsmBehaviour.registerDefaultTransition(COLLECT_LOCATE_SILO, COLLECT_GOTO_UNTIL_SILO);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO_UNTIL_SILO, COLLECT_GOTO_UNTIL_SILO);
    fsmBehaviour.registerTransition(COLLECT_GOTO_UNTIL_SILO, COLLECT_LOCATE_SILO, 1);
    fsmBehaviour.registerTransition(COLLECT_GOTO_UNTIL_SILO, COLLECT_DEADLOCK_SILO, 2);
    fsmBehaviour.registerTransition(COLLECT_GOTO_UNTIL_SILO, COLLECT_DROPOFF, 3);

    fsmBehaviour.registerDefaultTransition(COLLECT_DROPOFF, COLLECT);
    fsmBehaviour.registerTransition(COLLECT_DROPOFF, COLLECT_LOCATE_SILO, 1);

    fsmBehaviour.registerDefaultTransition(COLLECT_DEADLOCK, COLLECT_GOTO);
    fsmBehaviour.registerDefaultTransition(COLLECT_DEADLOCK_SILO, COLLECT_GOTO_UNTIL_SILO);

    // communication transitions
    fsmBehaviour.registerDefaultTransition(COMMUNICATION_SHAREMAP, COMMUNICATION_PLANEXPLORATION);
    fsmBehaviour.registerDefaultTransition(COMMUNICATION_PLANEXPLORATION, EXPLORE_GOTO);

    // waiting transitions
    fsmBehaviour.registerDefaultTransition(WAIT_UNTIL_MEETING, WAIT_COMMUNICATION);
    fsmBehaviour.registerTransition(WAIT_UNTIL_MEETING, COLLECT, 1); // Search for

    fsmBehaviour.registerDefaultTransition(WAIT_COMMUNICATION, WAIT_UNTIL_MEETING);
    fsmBehaviour.registerTransition(WAIT_COMMUNICATION, COLLECT, 1); // Follow me
    fsmBehaviour.registerTransition(WAIT_COMMUNICATION, ASKED_MOVE, 2); // Move aside
    
    // move aside transitions
    fsmBehaviour.registerDefaultTransition(ASKED_MOVE, MOVEASIDE);
    fsmBehaviour.registerDefaultTransition(MOVEASIDE, MOVEASIDE_GOTO);
    fsmBehaviour.registerTransition(MOVEASIDE_GOTO, WAIT_UNTIL_MEETING, 1);
    fsmBehaviour.registerTransition(MOVEASIDE_GOTO, MOVEASIDE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(MOVEASIDE_DEADLOCK, MOVEASIDE_GOTO);

    // meetig point transitions
    fsmBehaviour.registerDefaultTransition(MEETING, MEETING_GOTO);
    fsmBehaviour.registerTransition(MEETING, EXPLORE, 2);

    fsmBehaviour.registerDefaultTransition(MEETING_GOTO, MEETING_GOTO);
    fsmBehaviour.registerTransition(MEETING_GOTO, WAIT_UNTIL_MEETING, 1);
    fsmBehaviour.registerTransition(MEETING_GOTO, MEETING_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(MEETING_DEADLOCK, MEETING_GOTO);

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
