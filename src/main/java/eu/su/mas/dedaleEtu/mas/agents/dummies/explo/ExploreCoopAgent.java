package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.Memory;
import jade.core.behaviours.Behaviour;
import java.util.ArrayList;
import java.util.List;

// ExploreCoopAgent manages cooperative exploration between multiple agents in
// a Dedale environment. It initializes with a list of cooperating agents and
// manages map representation and exploration behavior.
public class ExploreCoopAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -7969469610241668140L;

  private Knowledge knowledge;
  private Memory memory;

  // setup initializes the agent with required behaviors and collaborator list.
  // Arguments must include at least the agent names to cooperate with.
  protected void setup() {
    super.setup();

    this.knowledge = new Knowledge();
    this.memory = new Memory(512);

    final Object[] args = getArguments();
    if (args.length == 0) {
      System.err.println("Error while creating agent");
      System.exit(-1);
    }

    List<String> agentNames = new ArrayList<String>();
    for (int i = 2; i < args.length; i++) {
      agentNames.add((String) args[i]);
    }

    List<Behaviour> behaviours = new ArrayList<Behaviour>();
    behaviours.add(new ExploCoopBehaviour(this, this.knowledge, agentNames, this.memory));
    behaviours.add(new ShareMapBehaviour(this, this.knowledge, agentNames, this.memory));

    addBehaviour(new StartMyBehaviours(this, behaviours));
  }

  // takeDown performs cleanup when the agent is destroyed
  protected void takeDown() {
    super.takeDown();
  }

  // beforeMove executes actions before agent migration between containers
  protected void beforeMove() {
    super.beforeMove();
  }

  // afterMove executes actions after agent migration between containers
  protected void afterMove() {
    super.afterMove();
  }
}
