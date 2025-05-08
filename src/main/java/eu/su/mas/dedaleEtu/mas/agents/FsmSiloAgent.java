package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.InitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CommunicationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareBrainBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.CollectSiloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PlanExplorationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SetMeetingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FsmSiloAgent extends AbstractDedaleAgent {
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

  private static final String COLLECT_LOCATE = "COLLECT";
  private static final String COLLECT_GOTO = "COLLECT_GOTO";
  private static final String COLLECT_WAIT = "COLLECT_WAIT";
  private static final String COLLECT_COMM = "COLLECT_COMM";
  private static final String COLLECT_COMM_SHARE = "COLLECT_COMM_SHARE";
  private static final String COLLECT_COMM_MEETING = "COLLECT_COMM_MEETING";
  private static final String COLLECT_DEADLOCK = "COLLECT_DEADLOCK";
  private static final String COLLECT_GOTO_DEADLOCK = "COLLECT_GOTO_DEADLOCK";

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
            put("pleasemove", -1);
          }
        }), EXPLORE_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(EXPLORE_COMM_SHARE, this, this.brain), EXPLORE_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(EXPLORE_COMM_MEETING, this, this.brain), EXPLORE_COMM_MEETING);
    fsmBehaviour.registerState(new PlanExplorationBehaviour(EXPLORE_COMM_PLAN, this, this.brain), EXPLORE_COMM_PLAN);

    // Collect behaviours
    fsmBehaviour.registerState(new CollectSiloBehaviour(COLLECT_LOCATE, this, this.brain), COLLECT_LOCATE);
    fsmBehaviour.registerState(new GoToBehaviour(COLLECT_GOTO, this, this.brain), COLLECT_GOTO);
    fsmBehaviour.registerState(new WaitUntilBehaviour(COLLECT_WAIT, this, this.brain), COLLECT_WAIT);
    fsmBehaviour
        .registerState(new CommunicationBehaviour(COLLECT_COMM, this, this.brain, 1, new HashMap<String, Integer>() {
          {
            put("sharemap", 1);
            put("pleasemove", -1);
          }
        }), COLLECT_COMM);
    fsmBehaviour.registerState(new ShareBrainBehaviour(COLLECT_COMM_SHARE, this, this.brain), COLLECT_COMM_SHARE);
    fsmBehaviour.registerState(new SetMeetingBehaviour(COLLECT_COMM_MEETING, this, this.brain), COLLECT_COMM_MEETING);
    fsmBehaviour.registerState(new DeadlockBehaviour(COLLECT_DEADLOCK, this, this.brain), COLLECT_DEADLOCK);
    fsmBehaviour.registerState(new GoToBehaviour(COLLECT_GOTO_DEADLOCK, this, this.brain), COLLECT_GOTO_DEADLOCK);

    fsmBehaviour.registerLastState(new EndBehaviour(END, this, this.brain), END);

    // - Transitions -
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE_GOTO);

    // Init transitions
    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE, COLLECT_LOCATE, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_COMM);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(EXPLORE_COMM, EXPLORE_DEADLOCK, -1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_SHARE, EXPLORE_COMM_MEETING);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_MEETING, EXPLORE_COMM_PLAN);
    fsmBehaviour.registerDefaultTransition(EXPLORE_COMM_PLAN, EXPLORE_GOTO);

    fsmBehaviour.registerDefaultTransition(EXPLORE_DEADLOCK, EXPLORE_GOTO);

    // Collect transitions
    fsmBehaviour.registerDefaultTransition(COLLECT_LOCATE, COLLECT_GOTO);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_WAIT, 1);
    fsmBehaviour.registerTransition(COLLECT_GOTO, COLLECT_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(COLLECT_WAIT, COLLECT_COMM);

    fsmBehaviour.registerDefaultTransition(COLLECT_COMM, COLLECT_WAIT);
    fsmBehaviour.registerTransition(COLLECT_COMM, COLLECT_COMM_SHARE, 1);
    fsmBehaviour.registerTransition(COLLECT_COMM, COLLECT_DEADLOCK, -1);

    fsmBehaviour.registerDefaultTransition(COLLECT_COMM_SHARE, COLLECT_COMM_MEETING);

    fsmBehaviour.registerDefaultTransition(COLLECT_COMM_MEETING, COLLECT_WAIT);

    fsmBehaviour.registerDefaultTransition(COLLECT_DEADLOCK, COLLECT_GOTO_DEADLOCK);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO_DEADLOCK, COLLECT_GOTO_DEADLOCK);
    fsmBehaviour.registerTransition(COLLECT_GOTO_DEADLOCK, COLLECT_LOCATE, 1);
    fsmBehaviour.registerTransition(COLLECT_GOTO_DEADLOCK, COLLECT_DEADLOCK, 2);

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
