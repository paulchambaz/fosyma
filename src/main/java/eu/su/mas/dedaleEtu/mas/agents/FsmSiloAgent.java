package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

import eu.su.mas.dedaleEtu.mas.behaviours.InitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreBehaviour;

import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

import java.util.ArrayList;
import java.util.List;

public class FsmSiloAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -78659868426454587L;

  private Knowledge knowledge;

  private final int agentSpeed = 10; // in nodes per seconds

  private static final String INIT = "Init";
  private static final String EXPLORE = "Explore";
  private static final String EXPLORE_GOTO = "Explore Go To";
  private static final String END = "End";

  protected void setup() {
    super.setup();

    this.knowledge = new Knowledge(this.getLocalName());

    int waitTime = 1000 / agentSpeed;

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

    // register behaviours
    fsmBehaviour.registerFirstState(new InitBehaviour(this, this.knowledge), INIT);
    fsmBehaviour.registerState(new ExploreBehaviour(this, this.knowledge), EXPLORE);
    fsmBehaviour.registerState(new GoToBehaviour(this, this.knowledge), EXPLORE_GOTO);
    fsmBehaviour.registerLastState(new EndBehaviour(this, this.knowledge), END);

    // register transitions
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE);

    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE, END, 1);

    fsmBehaviour.registerDefaultTransition(EXPLORE_GOTO, EXPLORE_GOTO);
    fsmBehaviour.registerTransition(EXPLORE_GOTO, EXPLORE, 1);

    List<Behaviour> behaviours = new ArrayList<Behaviour>();
    behaviours.add(fsmBehaviour);
    addBehaviour(new StartMyBehaviours(this, behaviours));
  }

  protected void takeDown() {
    super.takeDown();
  }

  protected void beforeMove() {
    this.knowledge.beforeMove();
    super.beforeMove();
  }

  protected void afterMove() {
    super.afterMove();
    this.knowledge.afterMove();
  }
}
