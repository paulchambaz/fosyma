package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.Memory;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.lang.acl.ACLMessage;
import java.util.List;

public class ShareMapBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = -568863390879327961L;
  private boolean finished = false;

  private Knowledge knowledge;
  // private Memory memory;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "share-map";

  public ShareMapBehaviour(Agent agent, Knowledge knowledge, List<String> receiverAgents, Memory memory) {
    super(agent);
    this.knowledge = knowledge;
    // this.memory = memory;
  }

  @Override
  public void action() {
    Communication comms = Protocols.handshake(this.myAgent, TIMEOUT, PROTOCOL_NAME);

    if (comms == null) {
      return;
    }

    if (comms.getProtocol() != PROTOCOL_NAME) {
      // TODO: it would be smart if we rerouted that behaviour to the
      // appropriate protocol
      return;
    }

    AID friend = comms.getFriend();
    boolean speaker = comms.shouldSpeak();

    for (int i = 0; i < 2; i++) {
      if (speaker) {
        // we send the map
        ACLMessage message = Utils.createACLMessage(
            this.myAgent,
            PROTOCOL_NAME,
            friend,
            this.knowledge.getSerializableKnowledge());
        ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
      } else {
        // we receive the map
        MessageTemplate filter = MessageTemplate.and(
            MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchProtocol("share-map")),
            MessageTemplate.MatchSender(friend));

        ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

        SerializableKnowledge knowledgeReceived = null;
        try {
          knowledgeReceived = (SerializableKnowledge) message.getContentObject();
        } catch (UnreadableException e) {
          e.printStackTrace();
        }
        this.knowledge.mergeKnowledge(knowledgeReceived);
      }
      speaker = !speaker;
    }
  }

  @Override
  public boolean done() {
    return finished;
  }
}
