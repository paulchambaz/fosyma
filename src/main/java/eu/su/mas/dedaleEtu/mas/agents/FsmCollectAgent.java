package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

import eu.su.mas.dedaleEtu.mas.behaviours.KnowledgeVisualizationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.InitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GoToGoalBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ComputeClosestTreasureBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PickSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ComputePathToSiloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DropOffBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;

import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

import java.util.ArrayList;
import java.util.List;

public class FsmCollectAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -78659868426454587L;

  private Knowledge knowledge;

  private final int agentSpeed = 10; // in nodes per seconds


  private static final String INIT = "Init";
  private static final String EXPLORE = "Explore";
  private static final String COMPUTETREASURE = "Compute Treasure";
  private static final String GOTO = "Go To Goal";
  private static final String PICKSOLO = "Pick Solo";
  private static final String COMPUTESILO = "Compute Silo";
  private static final String DROPOFF = "Drop Off";
  private static final String END = "End";
  

  protected void setup() {
    super.setup();

    this.knowledge = new Knowledge(this.getLocalName());

    int waitTime = 1000 / agentSpeed;

    FSMBehaviour fsmBehaviour = new FSMBehaviour();

    // register behaviours
    fsmBehaviour.registerFirstState(new InitBehaviour(this, this.knowledge), INIT);
    fsmBehaviour.registerState(new ExploCoopBehaviour(this, this.knowledge), EXPLORE);
    fsmBehaviour.registerState(new ComputeClosestTreasureBehaviour(this, this.knowledge), COMPUTETREASURE);
    fsmBehaviour.registerState(new GoToGoalBehaviour(this, this.knowledge), GOTO);
    fsmBehaviour.registerState(new PickSoloBehaviour(this, this.knowledge), PICKSOLO);
    fsmBehaviour.registerState(new ComputePathToSiloBehaviour(this, this.knowledge), COMPUTESILO);
    fsmBehaviour.registerState(new DropOffBehaviour(this, this.knowledge), DROPOFF);
    fsmBehaviour.registerLastState(new EndBehaviour(this, this.knowledge), END);

    // register transitions
    fsmBehaviour.registerDefaultTransition(INIT, EXPLORE);
    fsmBehaviour.registerDefaultTransition(EXPLORE, EXPLORE);
    fsmBehaviour.registerTransition(EXPLORE, COMPUTETREASURE, 1);
    fsmBehaviour.registerTransition(COMPUTETREASURE, GOTO, 1);

    fsmBehaviour.registerDefaultTransition(GOTO, GOTO);
    fsmBehaviour.registerTransition(GOTO, PICKSOLO, 1);
    fsmBehaviour.registerTransition(GOTO, DROPOFF, 2);

    fsmBehaviour.registerTransition(PICKSOLO, COMPUTESILO, 1);
    fsmBehaviour.registerTransition(COMPUTESILO, GOTO, 1);
    
    fsmBehaviour.registerTransition(DROPOFF, END, 1);

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
