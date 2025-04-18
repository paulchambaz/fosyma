package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class InitBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -374637573871453865L;

  private Brain brain;

  public InitBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Init");

    this.brain.createVisualization();

    brain.observe(this.myAgent);
    brain.selfLearn(this.myAgent);

    // this is all we need for collecting
    // boolean openLock(Observation o)
    // int pick()
    // boolean EmptyMyBackPack(String agentSiloName)

    // here are the things that we should set at this moment
  }
}
