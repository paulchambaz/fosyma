package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.Agent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedaleEtu.princ.Utils;

public class SetMeetingBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -537837587358659414L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "Set Meeting Point";

  public SetMeetingBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    Communication comms = brain.mind.getCommunication();

    if (comms.shouldSpeak()) {
      String meetingPoint = brain.entities.getPosition();

      ACLMessage message = Utils.createACLMessage(
          this.myAgent, PROTOCOL_NAME, comms.getFriend(), meetingPoint);
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);

      brain.entities.updateAgentMeetingPoint(comms.getFriend().getLocalName(),
          meetingPoint);
    } else {

      MessageTemplate filter = MessageTemplate.and(
          MessageTemplate.and(
              MessageTemplate.MatchPerformative(ACLMessage.INFORM),
              MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
          MessageTemplate.MatchSender(comms.getFriend()));

      ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

      if (message == null) {
        return;
      }

      String meetingPoint = null;
      try {
        meetingPoint = (String) message.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      brain.entities.updateAgentMeetingPoint(comms.getFriend().getLocalName(),
          meetingPoint);
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
