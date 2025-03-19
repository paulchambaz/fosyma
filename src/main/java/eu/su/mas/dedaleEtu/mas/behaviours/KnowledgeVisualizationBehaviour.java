package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class KnowledgeVisualizationBehaviour extends SimpleBehaviour {
  private boolean finished;
  private Knowledge knowledge;

  public KnowledgeVisualizationBehaviour(Knowledge knowledge) {
    this.finished = false;
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    this.knowledge.createVisualization();
    this.finished = true;
  }

  @Override
  public boolean done() {
    return finished;
  }
}
