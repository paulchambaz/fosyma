package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.Iterator;
import java.util.List;

// ExploCoopBehaviour implements cooperative exploration logic for agents
// to discover and map an environment while sharing topological information.
public class ExploCoopBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = 8567689731496787661L;

  private boolean finished = false;
  private MapRepresentation myMap;
  private List<String> list_agentNames;

  // ExploCoopBehaviour constructor initializes the exploration behavior with
  // a reference to the agent, its map representation, and cooperating agents.
  public ExploCoopBehaviour(
    final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames
  ) {
    super(myagent);
    this.myMap = myMap;
    this.list_agentNames = agentNames;
  }

  // action performs one step of the exploration algorithm:
  // - initializes map if needed
  // - collects observations from current position
  // - updates map with new nodes and edges
  // - processes topology info from other agents
  // - moves to next unexplored location
  @Override
  public void action() {
    if (this.myMap == null) {
      this.myMap = new MapRepresentation();
      this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent, 500, this.myMap, list_agentNames));
    }

    Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

    if (myPosition == null) {
      return;
    }

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();

    try {
      this.myAgent.doWait(1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

    String nextNodeId = null;
    Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = observations.iterator();
    while (iter.hasNext()) {
      Location accessibleNode = iter.next().getLeft();
      boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());

      if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
        this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
        if (nextNodeId == null && isNewNode) nextNodeId = accessibleNode.getLocationId();
      }
    }

    if (!this.myMap.hasOpenNode()) {
      finished = true;
      System.out.println(this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
      return;
    }

    if (nextNodeId == null) {
      nextNodeId = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
    }

    MessageTemplate msgTemplate = MessageTemplate.and(
            MessageTemplate.MatchProtocol("SHARE-TOPO"),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
    );

    ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
    if (msgReceived != null) {
      SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
      try {
        sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
      } catch (UnreadableException e) {
        e.printStackTrace();
      }
      this.myMap.mergeMap(sgreceived);
    }

    ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
  }

  // done signals when the exploration is complete by checking
  // if all nodes in the environment have been visited.
  @Override
  public boolean done() {
    return finished;
  }
}
