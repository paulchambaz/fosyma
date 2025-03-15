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

    while (ACLMessage message = getMessage("ping")) {
      // TODO: we should skip the ping phase since we have already an
      // interlocutor to talk to
      // TODO: even better  we should filter messages by the id of the agents
      // from the receivers
      //
      int hashPing;
      try {
        hashPing = Integer.parseInt(messageReceivedPing.getContent());
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }

    int hash = this.memory.addHash(this.knowledge.getSerializableKnowledge());

    ACLMessage message = Utils.createACLMessage(
      this.myAgent,
      "ping",
      receivers,
      String.valueOf(hash)
    );
    ((AbstractDedaleAgent) this.myAgent).sendMessage(msgPing);

    // TODO: maybe wrong protocol
    ACLMessage messageReceivedPing = waitForMessage("request-send-map", 100);

    // since no message was received before the timeout, we can assume the
    // other agent is gone and go do something else with our life
    if (messageReceivedPing == null) {
      return;
    }

    if (this.memory.hasHash(hashPing)) {
      // our knowledge is fresher than the agent we are talking to
      // we should help them get more informed by sending the map
      ACLMessage message = Utils.createACLMessage(
        this.myAgent,
        "knowledge-exchange",
        receivers,
        this.knowledge.getSerializedKnowledge()
      );
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    } else {
      // we have not been able to find the knowledge representation in our memory
      // we should first ask them if their knowledge is fresher that ours before continuing
      ACLMessage message = Utils.createACLMessage(
        this.myAgent,
        // TODO: this is not exactly the same ping be careful
        "ping",
        receivers,
        String.valueOf(hash)
      );
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    }

    // TODO: here we should do an action instead of listening
    ACLMessage messageReceivedPing = waitForMessage("request-send-map", 100);
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

  private ACLMessage getMessage(String protocol) {
    return this.myAgent.receive(
      MessageTemplate.and(
        MessageTemplate.MatchProtocol(protocol),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM)
    ));
  }

  private ACLMessage waitForMessage(String protocol, int timeout) {
    return this.myAgent.blockingReceive(
      MessageTemplate.and(
        MessageTemplate.MatchProtocol(protocol),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM)
    ), timeout); // blocking for at most 100ms
  }

  @Override
  public boolean done() {
    return finished;
  }
}
