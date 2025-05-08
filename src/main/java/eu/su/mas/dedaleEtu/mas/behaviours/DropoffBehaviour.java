package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Utils;

import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class DropoffBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -383953836736159459L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  public DropoffBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    brain.observe(this.myAgent);

    brain.log("i want to empty my backpack now");

    List<AID> silos = Utils.getSilos(this.myAgent);
    boolean success = false;
    for (AID silo : silos) {
      success = ((AbstractDedaleAgent) this.myAgent).emptyMyBackPack(silo.getLocalName());
      if (success) {
        break;
      }
    }

    if (success) {
      brain.entities.getMyself().emptyBackpack();
      brain.log("i succeeded in depositing the gold to the silo");
      this.exitValue = 0;
    } else {
      brain.log("i failed in depositing the gold to the silo");
      this.exitValue = 1;
    }

    brain.observe(this.myAgent);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
