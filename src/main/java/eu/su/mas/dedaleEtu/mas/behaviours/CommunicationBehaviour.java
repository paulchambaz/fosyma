package eu.su.mas.dedaleEtu.mas.behaviours;

// import java.util.HashMap;
import java.util.Map;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;

public class CommunicationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -373375987457439285L;

  private int exitValue = 0;

  private Map<String, Integer> routes;

  private Brain brain;

  public CommunicationBehaviour(Agent agent, Brain brain, Map<String, Integer> routes) {
    super(agent);
    this.brain = brain;
    this.routes = routes;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Explore Communication");

    Communication comms = Protocols.handshake(this.myAgent, brain, 100, "sharemap");

    if (comms == null) {
      this.exitValue = 0;
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
