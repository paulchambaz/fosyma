package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Map;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;

public class ExclusiveCommunicationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -373375987457439285L;

  private String state;
  private int exitValue = 0;

  private String protocol;
  private int priority;
  private Map<String, Integer> routes;

  private Brain brain;

  public ExclusiveCommunicationBehaviour(String state, Agent agent, Brain brain, int priority,
      Map<String, Integer> routes) {
    super(agent);
    this.brain = brain;
    this.routes = routes;
    this.priority = priority;
    this.state = state;

    for (Map.Entry<String, Integer> entry : routes.entrySet()) {
      if (entry.getValue() == 1) {
        this.protocol = entry.getKey();
      }
    }
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    Communication comms = Protocols.exclusiveHandshake(this.myAgent, brain, 100, protocol, priority,
        brain.mind.getCoordinationPartner());

    if (comms == null) {
      brain.mind.incrementStuckCounter();
      if (brain.mind.isStuck()) {
        this.exitValue = -1;
      } else {
        this.exitValue = 0;
      }
      return;
    }

    if (!this.routes.containsKey(comms.getProtocol())) {
      this.exitValue = 0;
      return;
    }

    brain.mind.setCommunication(comms);
    brain.mind.resetSocialCooldown();
    this.exitValue = this.routes.get(comms.getProtocol());
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
