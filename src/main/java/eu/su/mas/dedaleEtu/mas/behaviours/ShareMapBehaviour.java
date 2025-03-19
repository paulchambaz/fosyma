package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableKnowledge;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.lang.acl.ACLMessage;

public class ShareMapBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = -568863390879327961L;
  private boolean finished = false;

  private Knowledge knowledge;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "share-map";

  public ShareMapBehaviour(Agent agent, Knowledge knowledge) {
    super(agent);
    this.knowledge = knowledge;
  }

  @Override
  public void action() {
    Communication comms = Protocols.handshake(this.myAgent, TIMEOUT, PROTOCOL_NAME);

    if (comms == null) {
      return;
    }

    System.out.println(
        this.myAgent.getLocalName() + " - result of the handshake protocol : " + comms.getFriend().getLocalName());

    if (!comms.getProtocol().equals(PROTOCOL_NAME)) {
      // TODO: we should reroute behaviour when changing protocols
      return;
    }

    AID friend = comms.getFriend();
    boolean speaker = comms.shouldSpeak();

    for (int i = 0; i < 2; i++) {
      if (speaker) {
        sendKnowledge(friend);
      } else {
        receiveKnowledge(friend);
      }
      speaker = !speaker;
    }
  }

  private void sendKnowledge(AID friend) {
    ACLMessage message = Utils.createACLMessage(
        this.myAgent,
        PROTOCOL_NAME,
        friend,
        this.knowledge.getSerializableKnowledge());
    ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
  }

  private void receiveKnowledge(AID friend) {
    MessageTemplate filter = MessageTemplate.and(
        MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
        MessageTemplate.MatchSender(friend));

    ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

    if (message == null) {
      return;
    }

    SerializableKnowledge knowledgeReceived = null;
    try {
      knowledgeReceived = (SerializableKnowledge) message.getContentObject();
    } catch (UnreadableException e) {
      e.printStackTrace();
      return;
    }

    System.out.println(this.myAgent.getLocalName() + " just received knowledge");

    this.knowledge.mergeKnowledge(knowledgeReceived);
  }

  @Override
  public boolean done() {
    return finished;
  }
}
