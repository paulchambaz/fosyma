package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.List;

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
    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

    msg.setProtocol("SHARE-TOPO");
    msg.setSender(this.myAgent.getAID());

    for (String agentName : receiverAgents) {
      msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
    }

    SerializableSimpleGraph<String, MapAttribute> serializableGraph = this.myMap.getSerializableGraph();

    try {
      msg.setContentObject(serializableGraph);
    } catch (IOException e) {
      e.printStackTrace();
    }

    ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
  }

  @Override
  public boolean done() {
    return finished;
  }
}
