package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentData;

import java.util.Map;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class OpenLockBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -385943593446598313L;

  private int exitValue = 0;

  private Brain brain;

  public OpenLockBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Open lock");
    brain.observe(this.myAgent);

    String position = brain.entities.getPosition();
    TreasureData treasure = brain.entities.getTreasures().get(position);

    if (treasure == null) {
      return;
    }

    brain.log(treasure);
    brain.log(treasure.isLocked());
    if (!treasure.isLocked()) {
      return;
    }

    AgentData myself = brain.entities.getMyself();
    if (!myself.canOpenLock(treasure.getLockpickStrength())) {
      int deficit = calculateLockpickDeficit(treasure, myself);
      brain.log("Cannot open lock - lockpicking deficit:", deficit);
      brain.mind.setCoordinationTreasureNode(position);
      this.exitValue = 1;
      return;
    }

    boolean success = ((AbstractDedaleAgent) this.myAgent).openLock(brain.entities.getMyself().getTreasureType());
    if (success) {
      treasure.setLocked(false);
      brain.log(treasure);
      brain.log(treasure.isLocked());
    }

    this.brain.observe(this.myAgent);
    this.exitValue = 0;
  }

  private int calculateLockpickDeficit(TreasureData treasure, AgentData agent) {
    int requiredStrength = treasure.getLockpickStrength();
    int agentStrength = 0;

    Map<Observation, Integer> expertise = agent.getExpertise();
    if (expertise.containsKey(Observation.LOCKPICKING)) {
      agentStrength = expertise.get(Observation.LOCKPICKING);
    }

    return Math.max(0, requiredStrength - agentStrength);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
