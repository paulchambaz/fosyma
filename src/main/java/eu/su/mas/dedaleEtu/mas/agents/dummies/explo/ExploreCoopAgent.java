package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.KnowledgeVisualizationBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import jade.core.behaviours.Behaviour;
import java.util.ArrayList;
import java.util.List;

// ExploreCoopAgent manages cooperative exploration between multiple agents in
// a Dedale environment. It initializes with a list of cooperating agents and
// manages map representation and exploration behavior.
public class ExploreCoopAgent extends AbstractDedaleAgent {
  private static final long serialVersionUID = -7969469610241668140L;

  private Knowledge knowledge;

  // setup initializes the agent with required behaviors and collaborator list.
  // Arguments must include at least the agent names to cooperate with.
  protected void setup() {
    super.setup();

    this.knowledge = new Knowledge(this.getLocalName());

    List<Behaviour> behaviours = new ArrayList<Behaviour>();
    behaviours.add(new ExploCoopBehaviour(this, this.knowledge));
    behaviours.add(new ShareMapBehaviour(this, this.knowledge));
    behaviours.add(new KnowledgeVisualizationBehaviour(this.knowledge));

    addBehaviour(new StartMyBehaviours(this, behaviours));
  }

  // takeDown performs cleanup when the agent is destroyed
  protected void takeDown() {
    super.takeDown();
  }

  // beforeMove executes actions before agent migration between containers
  protected void beforeMove() {
    this.knowledge.beforeMove();
    super.beforeMove();
  }

  // afterMove executes actions after agent migration between containers
  protected void afterMove() {
    super.afterMove();
    this.knowledge.afterMove();
  }
}
