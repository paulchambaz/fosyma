package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;

public class ShareMapBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -568863390879327961L;

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "sharemap";

  public ShareMapBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Share Map");

    Communication comms = brain.mind.getCommunication();

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
    ACLMessage message = Utils.createACLMessage(
        this.myAgent,
        PROTOCOL_NAME,
        friend,
        this.brain.map.getSerializableGraph());
    ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    brain.log("just shared brain with", friend.getLocalName());
  }

  private void receiveBrain(AID friend) {
    MessageTemplate filter = MessageTemplate.and(
        MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
        MessageTemplate.MatchSender(friend));

    ACLMessage message = this.myAgent.blockingReceive(filter, 100 * TIMEOUT);

    if (message == null) {
      return;
    }

    SerializableSimpleGraph<String, MapAttribute> brainReceived = null;
    try {
      brainReceived = (SerializableSimpleGraph<String, MapAttribute>) message.getContentObject();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    brain.log("just received brain from", friend.getLocalName());
    this.brain.map.mergeWithReceivedMap(brainReceived);
  }
}
