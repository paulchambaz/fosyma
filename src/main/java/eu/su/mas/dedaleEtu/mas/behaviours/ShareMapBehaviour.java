package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class ShareMapBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = -568863390879327961L;
  private boolean finished = false;

  private Knowledge knowledge;
  private List<String> receiverAgents;
  private Memory memory;

  public ShareMapBehaviour(Agent agent, Knowledge knowledge, List<String> receiverAgents, Memory memory) {
    super(agent);
    this.knowledge = knowledge;
    this.receiverAgents = receiverAgents;
    this.memory = memory;
  }

  @Override
  public void action() {
    Communication comms = Protocols.handshake(this.myAgent, 100, "share-map");

    if (comms == null) {
      return;
    }

    if (comms.getProtocol() != "share-map") {
      // TODO: it would be smart if we rerouted that behaviour to the
      // appropriate protocol
      return;
    }

    String friend = comms.getFriend();

    if (comms.shouldSpeak()) {
      // we want to send our hash
    } else {
      // we want to receive the hash of our friend
    }
  }

  // private List<String> getReceivers() {
  //   List<String> receivers = new ArrayList<>();
  //
  //   List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();
  //
  //   for (Couple<Location, List<Couple<Observation, String>>> observation : observations ) {
  //     List<Couple<Observation, String>> nodeObservations = observation.getRight();
  //
  //     // Test si un agent est observé à la position
  //     for (Couple<Observation, String> iter : nodeObservations){
  //       if(iter.getLeft() == Observation.AGENTNAME){
  //         receivers.add(iter.getRight());
  //       }
  //     }
  //   }
  //   receivers = new ArrayList<>(new HashSet<>(receivers));
  //   return receivers;
  // }

  // private Integer decodeHash(ACLMessage messageReceived) {
  //     int hashPing;
  //     try {
  //       return Integer.parseInt(messageReceived.getContent());
  //     } catch (NumberFormatException e) {
  //       e.printStackTrace();
  //       return null;
  //     }
  // }

  // private Couple<String, ACLMessage> getMessage(String protocol, List<String> receivers) {
  //   for (String receiver : receivers) {
  //     AID agentID = receiver.getAID();
  //     MessageTemplate template = MessageTemplate.and(
  //       MessageTemplate.MatchProtocol(protocol),
  //       MessageTemplate.MatchPerformative(ACLMessage.INFORM),
  //       MessageTemplate.MatchSender(agentID)
  //     );
  //
  //     ACLMessage message = this.myAgent.receive(template);
  //     if (message != null) { return new Couple<>(receiver, message); }
  //   }
  //
  //   return null;
  // }

  // private ACLMessage waitForMessage(List<String> protocols, List<String> receivers, int timeout) {
  //   MessageTemplate protocolsFilter = null;
  //   for (String protocol : protocols) {
  //     if (protocolsFilter == null) {
  //       protocolsFilter = MessageTemplate.MatchProtocol(protocol);
  //     } else {
  //       protocolsFilter = MessageTempate.or(
  //         protocolsFilter,
  //         MessageTemplate.MatchProtocol(protocol)
  //       );
  //     }
  //   }
  //
  //   MessageTemplate receiversFilter = null;
  //   for (String receiver : recivers) {
  //     AID agentID = receiver.getAID();
  //     if (receiversFilter == null) {
  //       receiversFilter = MessageTemplate.MatchSender(agentID);
  //     } else {
  //       receiversFilter = MessageTemplate.or(
  //         receiversFilter,
  //         MessageTemplate.MatchSender(agentID);
  //       );
  //     }
  //   }
  //
  //   return this.myAgent.blockingReceive(
  //     MessageTemplate.and(
  //       MessageTemplate.MatchPerformative(ACLMessage.INFORM),
  //       protocolsFilter,
  //       receiversFilter
  //     ),
  //     timeout
  //   ); // blocking for at most 100ms
  // }

  @Override
  public boolean done() {
    return finished;
  }
}
