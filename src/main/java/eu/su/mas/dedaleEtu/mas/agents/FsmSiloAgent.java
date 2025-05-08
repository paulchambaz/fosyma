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
import eu.su.mas.dedaleEtu.mas.behaviours.WaitUntilBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.MoveAsideBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.AskToMoveBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FsmSiloAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -78659868426454587L;

  private Brain brain;

  private static final String INIT = "Init";

  private static final String EXPLORE = "Explore";
  private static final String EXPLORE_GOTO = "Explore Go To";
  private static final String EXPLORE_DEADLOCK = "Explore deadlock";
  private static final String EXPLORE_COMMUNICATION = "Explore communication";

  private static final String COLLECT_SILO = "Collect Silo";
  private static final String COLLECT_GOTO = "Collect Go To";

  private static final String COMMUNICATION_SHAREMAP = "Communication share map";

  private static final String WAIT_COLLECTING = "Waiting for collection";

  private static final String MOVEASIDE = "Move Aside";
  private static final String MOVEASIDE_DEADLOCK = "Move Aside Deadlock";
  private static final String MOVEASIDE_GOTO = "Move Aside Go To";

  private static final String WAIT_COMMUNICATION = "Communication wait";
  private static final String ASKED_MOVE = "Asked Move";
  

  private static final String END = "End";

  protected void setup() {
    super.setup();

    this.brain = new Brain(this.getLocalName());

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

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
    fsmBehaviour.registerState(new CollectSiloBehaviour(this, this.brain), COLLECT_SILO);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), COLLECT_GOTO);

    // communication behaviours
    fsmBehaviour.registerState(new ShareBrainBehaviour(this, this.brain), COMMUNICATION_SHAREMAP);

    // end behaviours
    fsmBehaviour.registerLastState(new EndBehaviour(this, this.brain), END);

    // waiting behaviours
    fsmBehaviour.registerState(new WaitUntilBehaviour(this, this.brain, 1000000000), WAIT_COLLECTING);
    fsmBehaviour.registerState(new CommunicationBehaviour(this, this.brain, new HashMap<String, Integer>() {
      {
        put("move aside", 1);
      }
    }),
    WAIT_COMMUNICATION);

    fsmBehaviour.registerState(new AskToMoveBehaviour(this, this.brain), ASKED_MOVE);
    
    // move aside behaviours
    fsmBehaviour.registerState(new MoveAsideBehaviour(this, this.brain), MOVEASIDE);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.brain), MOVEASIDE_GOTO);
    fsmBehaviour.registerState(new DeadlockBehaviour(this, this.brain), MOVEASIDE_DEADLOCK);

    // init transitions
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE);

    // explore transitions
    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE, COLLECT_SILO, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_COMMUNICATION);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(EXPLORE_DEADLOCK, EXPLORE_GOTO);

    fsmBehaviour.registerDefaultTransition(EXPLORE_COMMUNICATION, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_COMMUNICATION, COMMUNICATION_SHAREMAP, 1);

    // collect transitions
    fsmBehaviour.registerDefaultTransition(COLLECT_SILO, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT_SILO, EXPLORE, 2);

    fsmBehaviour.registerDefaultTransition(COLLECT_GOTO, COLLECT_GOTO);
    fsmBehaviour.registerTransition(COLLECT_GOTO, WAIT_COLLECTING, 1);

    // waiting transitions
    fsmBehaviour.registerDefaultTransition(WAIT_COLLECTING, WAIT_COMMUNICATION);

    fsmBehaviour.registerDefaultTransition(WAIT_COMMUNICATION, WAIT_COLLECTING);
    fsmBehaviour.registerTransition(WAIT_COMMUNICATION, ASKED_MOVE, 1);

    // move aside transitions
    fsmBehaviour.registerDefaultTransition(ASKED_MOVE, MOVEASIDE);
    fsmBehaviour.registerDefaultTransition(MOVEASIDE, MOVEASIDE_GOTO);
    fsmBehaviour.registerTransition(MOVEASIDE_GOTO, WAIT_COLLECTING, 1);
    fsmBehaviour.registerTransition(MOVEASIDE_GOTO, MOVEASIDE_DEADLOCK, 2);

    fsmBehaviour.registerDefaultTransition(MOVEASIDE_DEADLOCK, MOVEASIDE_GOTO);

    // communication transitions
    fsmBehaviour.registerDefaultTransition(COMMUNICATION_SHAREMAP, EXPLORE_GOTO);

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
