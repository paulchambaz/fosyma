package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
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
    List<String> receivers = getReceivers();

    // we have no one to talk to so we can skip the rest of this behaviour
    if (receivers.isEmpty()) {
      return;
    }

    // TODO: we may want to change that since the communication radius is
    // larger than the observation radius

    boolean ok = handshake(receivers);

    if (!ok) {
      return;
    }

    // start of the protocol - we still need to do a little bit of busy work
    // first, we just sent a message - in one case we are awaiting a pair of
    // open nodes and neighbourhood and in the other we have to send a pair of
    // open nodes and neighbourhood

    // ok i still need to think
    // let's say that alice send a start-exchange-knowledge to bob with the list of open nodes
    // in this case what should alice do ? they should await for bob's response
    // in this case what should bob do ? they should compute the neighbourhood and send it to alice
    // the issue is in one case we should send and in the other we should await so we are not symmetrical
    // since we are writing this behaviour, both agent should be in the same state and both should have just received a list of open nodes
    // so that means two things :
    // 1. we should send our open nodes in the start-exchange-knowledge
    // 2. in the handshake that we do we should await for the first response
  }

  private boolean handshake(List<String> receivers) {
    Couple<String, ACLMessage> message = getMessage("ping", receivers);

    if (message != null) {
      // before sending anything, we have received some message from an agent
      // nearby - we will handle it directly
      Integer hashReceived = decodeHash(message.getRight());
      ACLMessage message;
      if (this.memory.hasHash(hashReceived)) {
        // our memory is fresher than the one of the other member so we should
        // share our map with the other member
        message = Utils.createACLMessage(
          this.myAgent,
          "knowledge-exchange",
          new List<String> { message.getLeft() },
          this.knowledge.getSerializedKnowledge()
        );
        ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
        return false;
      } else {
        // we are unsure about the situation of the other member so we should
        // send a new ping to notify the other member
        int hash = this.memory.addHash(this.knowledge.getSerializableKnowledge());
        message = Utils.createACLMessage(
          this.myAgent,
          "ping",
          new List<String> { message.getLeft() },
          String.valueOf(hash)
        );
        ((AbstractDedaleAgent) this.myAgent).sendMessage(message);

        // in this situation we have identified a single agent to exchange with
        // if we have contacted this agent, figured out we do not know more
        // than them and have sent them out state of knowledge, then two things
        // can happen, either they send us the map because they know more than
        // us or they send us the goto start exchanging information in a
        // symmetrical manner
        ACLMessage message = waitForMessage(
          new ArrayList<>{ "exchange-knowledge", "start-exchange-knowledge" },
          new ArrayList<>{ receiver },
          100
        );

        // if we have not received anything before the timeout, then we give up
        if (message == null) {
          return false;
        }

        // therefore the first thing we want to do after we receive the message
        // is identifying what happened, did we get the map or did we get the
        // start
        if (message.getProtocol() == "exchange-knowledge") {
          // we just want to merge our knowledge
          SerializableKnowledge knowledgeReceived = null;
          try {
            knowledgeReceived = (SerializableKnowledge) message.getContentObject();
          } catch (UnreadableException e) {
            e.printStackTrace();
          }
          this.knowledge.mergeKnowledge(knowledgeReceived);
          return false;
        }

        // if we dont we can be certain of two things :
        // 1. we have finished the handshake and are entering the next phase of
        // the protocol
        // 2. the other agent knows this too and is awaiting a openNodes and
        // neighbourhood pair
        return true;
      }
    } else {
      // we have not received any message from someone nearby, we will attempt
      // communication
      int hash = this.memory.addHash(this.knowledge.getSerializableKnowledge());
      message = Utils.createACLMessage(
        this.myAgent,
        "ping",
        receivers,
        String.valueOf(hash);
      );
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
      );

      // we dont know from whom but we should now await an answer from anyone
      // in the list of members closeby. A couple of answers are possible.
      // if we receive ping, that means the other agent was not more
      // knowledgable than us and is asking us where we are. if we have seen
      // their hash, we should sent them our map and if not we should initiate
      // the protocol
      //
      // there is a big question : what kind of protocols are we awaiting :
      // first can we receive ping : yes, in which case we can either send the map or start the protocol
      ACLMessage message = waitForMessage(
        new ArrayList<>{ "exchange-knowledge", "start-exchange-knowledge" },
        receivers,
        100
      );

      // if we have no response to the call, then it means that the other agent
      // went away too fast, in which case, we give up the rest of the protocol
      if (message == null) {
        return false;
      }

      if (message.getProtocol() == "ping") {
        Integer hashReceived = decodeHash(message);

        if (this.memory.hasHash(hashReceived)) {
          // our memory is fresher than the one of the other member so we should
          // share our map with the other member
          ACLMessage message = Utils.createACLMessage(
            this.myAgent,
            "knowledge-exchange",
            new List<String> { message.getSender() },
            this.knowledge.getSerializedKnowledge()
          );
          ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
          return false;
        }

        // if we dont we can be certain of two things :
        // 1. we have finished the handshake and are entering the next phase of
        // the protocol
        // 2. the other agent does not know this and we should tell them
        ACLMessage message = Utils.createACLMessage(
          this.myAgent,
          "start-knowledge-exchange",
          new List<String> { message.getSender() },
          null,
        );
        ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
        
        return true;
      }
    }
  }

  private List<String> getReceivers() {
    List<String> receivers = new ArrayList<>();

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();

    for (Couple<Location, List<Couple<Observation, String>>> observation : observations ) {
      List<Couple<Observation, String>> nodeObservations = observation.getRight();

      // Test si un agent est observé à la position
      for (Couple<Observation, String> iter : nodeObservations){
        if(iter.getLeft() == Observation.AGENTNAME){
          receivers.add(iter.getRight());
        }
      }
    }
    receivers = new ArrayList<>(new HashSet<>(receivers));
    return receivers;
  }

  private Integer decodeHash(ACLMessage messageReceived) {
      int hashPing;
      try {
        return Integer.parseInt(messageReceived.getContent());
      } catch (NumberFormatException e) {
        e.printStackTrace();
        return null;
      }
  }

  private Couple<String, ACLMessage> getMessage(String protocol, List<String> receivers) {
    for (String receiver : receivers) {
      AID agentID = receiver.getAID();
      MessageTemplate template = MessageTemplate.and(
        MessageTemplate.MatchProtocol(protocol),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchSender(agentID)
      );

      ACLMessage message = this.myAgent.receive(template);
      if (message != null) { return new Couple<>(receiver, message); }
    }

    return null;
  }

  private ACLMessage waitForMessage(List<String> protocols, List<String> receivers, int timeout) {
    MessageTemplate protocolsFilter = null;
    for (String protocol : protocols) {
      if (protocolsFilter == null) {
        protocolsFilter = MessageTemplate.MatchProtocol(protocol);
      } else {
        protocolsFilter = MessageTempate.or(
          protocolsFilter,
          MessageTemplate.MatchProtocol(protocol)
        );
      }
    }

    MessageTemplate receiversFilter = null;
    for (String receiver : recivers) {
      AID agentID = receiver.getAID();
      if (receiversFilter == null) {
        receiversFilter = MessageTemplate.MatchSender(agentID);
      } else {
        receiversFilter = MessageTemplate.or(
          receiversFilter,
          MessageTemplate.MatchSender(agentID);
        );
      }
    }

    return this.myAgent.blockingReceive(
      MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        protocolsFilter,
        receiversFilter
      ),
      timeout
    ); // blocking for at most 100ms
  }

  @Override
  public boolean done() {
    return finished;
  }
}
