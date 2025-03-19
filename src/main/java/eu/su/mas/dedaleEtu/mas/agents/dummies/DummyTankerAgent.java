package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import java.util.ArrayList;
import java.util.List;

public class DummyTankerAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -1784844593772918359L;

  protected void setup() {
    super.setup();

    List<Behaviour> lb = new ArrayList<Behaviour>();
    lb.add(new RandomTankerBehaviour(this));

    addBehaviour(new StartMyBehaviours(this, lb));

    System.out.println("the  agent " + this.getLocalName() + " is started");
  }

  protected void takeDown() {
    super.takeDown();
  }

  protected void beforeMove() {
    super.beforeMove();
  }

  protected void afterMove() {
    super.afterMove();
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
      System.out.println(this.myAgent.getLocalName() + " -- list of observables: " + lobs);
    }
  }
}
