package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.serializableGraph.SerializableNode;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableBrain;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;

public class ShareBrainBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -568863390879327961L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "sharemap";

  public ShareBrainBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

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
    List<String> knownNodesByFriend = brain.entities.getAgentKnownNodes(friend.getLocalName());
    List<String> nodes = brain.map.getNodes();

    List<String> nodesToShare = new ArrayList<>();

    for (String node : nodes) {
      if (!knownNodesByFriend.contains(node)) {
        nodesToShare.add(node);
      }
    }

    SerializableSimpleGraph<String, MapAttribute> partialGraph = brain.map.getSerializedSubgraphFromNodes(nodesToShare);

    SerializableBrain partialBrain = new SerializableBrain(brain);
    partialBrain.setGraph(partialGraph);

    ACLMessage message = Utils.createACLMessage(
        this.myAgent, PROTOCOL_NAME, friend, partialBrain);
    ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    // brain.log("just shared brain with", friend.getLocalName());
  }

  private void receiveBrain(AID friend) {
    String friendName = friend.getLocalName();

    MessageTemplate filter = MessageTemplate.and(
        MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
        MessageTemplate.MatchSender(friend));

    ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

    if (message == null) {
      return;
    }

    SerializableBrain brainReceived = null;
    try {
      brainReceived = (SerializableBrain) message.getContentObject();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    // brain.log("just received brain from", friend.getLocalName());
    brain.merge(brainReceived);

    SerializableSimpleGraph<String, MapAttribute> receivedGraph = brainReceived.getGraph();
    List<String> receivedNodes = new ArrayList<>();
    for (SerializableNode<String, MapAttribute> node : receivedGraph.getAllNodes()) {
      receivedNodes.add(node.getNodeId());
    }

    brain.entities.updateAgentKnownNodes(friendName, receivedNodes);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
