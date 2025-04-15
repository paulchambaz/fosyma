package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableKnowledge;
import dataStructures.serializableGraph.SerializableSimpleGraph;
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

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "share-map";

  public ShareMapBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    Communication comms = Protocols.handshake(this.myAgent, this.brain, TIMEOUT, PROTOCOL_NAME);

    if (comms == null) {
      return;
    }
    this.brain.mind.resetSocialCooldown();

    System.out.println(
        this.myAgent.getLocalName() + " - result of the handshake protocol : " + comms.getFriend().getLocalName());

    if (!(comms.getProtocol().equals(PROTOCOL_NAME))) {
      // TODO: we should reroute behaviour when changing protocols
      return;
    }

    AID friend = comms.getFriend();
    boolean speaker = comms.shouldSpeak();

    for (int i = 0; i < 2; i++) {
      if (speaker) {
        sendBrain(friend);
      } else {
        receiveBrain(friend);
      }
      speaker = !speaker;
    }
  }

  private void sendBrain(AID friend) {
    System.out.println(this.myAgent.getLocalName() + " is speaking");
    ACLMessage message = Utils.createACLMessage(
        this.myAgent,
        PROTOCOL_NAME,
        friend,
        // TODO; we should also send the entities
        this.brain.map.getSerializableGraph());
    ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    System.out.println(this.myAgent.getLocalName() + " just sent brain");
  }

  private void receiveBrain(AID friend) {
    MessageTemplate filter = MessageTemplate.and(
        MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
        MessageTemplate.MatchSender(friend));

    ACLMessage message = this.myAgent.blockingReceive(filter, 100 * TIMEOUT);

    if (message == null) {
      System.out.println(this.myAgent.getLocalName() + " did not receive any message - fail");
      return;
    }

    SerializableSimpleGraph<String, MapAttribute> brainReceived = null;
    try {
      brainReceived = (SerializableSimpleGraph<String, MapAttribute>) message.getContentObject();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.println(this.myAgent.getLocalName() + " just received brain");
    this.brain.map.mergeWithReceivedMap(brainReceived);
  }

  @Override
  public boolean done() {
    return finished;
  }
}
