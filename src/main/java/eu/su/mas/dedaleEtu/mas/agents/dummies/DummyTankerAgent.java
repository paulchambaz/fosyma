package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.KnowledgeVisualizationBehaviour;
import java.util.ArrayList;
import java.util.List;

public class DummyTankerAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -1784844593772918359L;

  private Knowledge knowledge;

  protected void setup() {
    super.setup();

    this.knowledge = new Knowledge(this.getLocalName());

    List<Behaviour> behaviours = new ArrayList<>();
    behaviours.add(new ExploCoopBehaviour(this, this.knowledge));
    behaviours.add(new ShareMapBehaviour(this, this.knowledge));
    behaviours.add(new KnowledgeVisualizationBehaviour(this.knowledge));

    addBehaviour(new StartMyBehaviours(this, behaviours));
  }

  protected void takeDown() {
    super.takeDown();
  }

  // beforeMove executes actions before agent migration between containers
  protected void beforeMove() {
    if (this.knowledge != null) {
      this.knowledge.prepareMigration();
    }
    super.beforeMove();
  }

  // afterMove executes actions after agent migration between containers
  protected void afterMove() {
    super.afterMove();
    if (this.knowledge != null) {
      this.knowledge.loadSavedData();
    }
  }
}

class RandomTankerBehaviour extends TickerBehaviour {
  private static final long serialVersionUID = 9088209402507795289L;

  public RandomTankerBehaviour(final AbstractDedaleAgent myagent) {
    super(myagent, 10000);
  }

  @Override
  public void onTick() {
    Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

    if (myPosition != null) {
      List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe(); // myPosition
      System.out.println(this.myAgent.getLocalName() + " -- list of observables: "
          + lobs);
    }
  }
}
