package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class ShareMapBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = -568863390879327961L;
  private boolean finished = false;

  private MapRepresentation myMap;
  private List<String> receiverAgents;

  public ShareMapBehaviour(Agent agent, MapRepresentation myMap, List<String> receiverAgents) {
    super(agent);
    this.myMap = myMap;
    this.receiverAgents = receiverAgents;
  }

  // this behaviour sends the full map
  @Override
  public void action() {
    // 4) At each time step, the agent blindly send all its graph to its surrounding to illustrate
    // how to share its knowledge (the topology currently) with the the others agents.
    // If it was written properly, this sharing action should be in a dedicated behaviour set, the
    // receivers be automatically computed, and only a subgraph would be shared.
    
    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();

    // Liste agents observés
    List<String> receivers = new ArrayList<String>();

    for (Couple<Location, List<Couple<Observation, String>>> observation : observations ) {
      List<Couple<Observation, String>> nodeObservations = observation.getRight();

      // Test si un agent est observé à la position
      for (Couple<Observation, String> iter : nodeObservations){
        if(iter.getLeft() == Observation.AGENTNAME){
          receivers.add(iter.getRight());
        }
      }
    }

    // Envoie de la carte
    if (!(receivers.isEmpty())){
      SerializableSimpleGraph<String, MapAttribute> sg = this.myMap.getSerializableGraph();
      int myHashCode = sg.hashCode();

      // Message de ping
      ACLMessage msgPing = new ACLMessage(ACLMessage.INFORM);
      msgPing.setSender(this.myAgent.getAID());
      msgPing.setProtocol("ASK-SHARE");
      msgPing.setContent(String.valueOf(myHashCode));
      
      for (String agentName : receivers) {
        msgPing.addReceiver(new AID(agentName, AID.ISLOCALNAME));
      }
      // System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
      ((AbstractDedaleAgent) this.myAgent).sendMessage(msgPing);
      
      // Reponse
      MessageTemplate msgTemplate = MessageTemplate.and(
            MessageTemplate.MatchProtocol("ASK-SHARE"),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
      );

      ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
      if (msgReceived != null) {
        int hashReceived = 0;
        try {
          hashReceived = Integer.parseInt(msgReceived.getContent());
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }

        if (hashReceived!= myHashCode){
        // Message d'envoie de carte
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("SHARE-TOPO");
        msg.setSender(this.myAgent.getAID());
        for (String agentName : receivers) {
          msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }

        try {
          msg.setContentObject(sg);
        } catch (IOException e) {
          e.printStackTrace();
        }
        // System.out.println("Agent "+this.myAgent.getLocalName()+ " is sending its map");
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
      }
      }
    }
  }

  @Override
  public boolean done() {
    return finished;
  }
}
