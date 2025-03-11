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

/**
 *
 * ExploBehaviour
 */
public class ExploBehaviour extends SimpleBehaviour {

  private static final long serialVersionUID = 8567689731496787661L;
  private boolean finished = false;

  /** Current knowledge of the agent regarding the environment */
  private MapRepresentation myMap;

  private List<String> list_agentNames;

  /**
   * @param myagent reference to the agent we are adding this behaviour to
   * @param myMap known map of the world the agent is living in
   * @param agentNames name of the agents to share the map with
   */
  public ExploBehaviour(
      final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
    super(myagent);
    this.myMap = myMap;
    this.list_agentNames = agentNames;
  }

  @Override
  public void action() {
    if (this.myMap == null) {
      this.myMap = new MapRepresentation();
      this.myAgent.addBehaviour(
          new ShareMapBehaviour(this.myAgent, 500, this.myMap, list_agentNames));
    }

    // 0) Retrieve the current position
    Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

    if (myPosition != null) {
      // List of observable from the agent's current position
      List<Couple<Location, List<Couple<Observation, String>>>> lobs =
          ((AbstractDedaleAgent) this.myAgent).observe(); // myPosition

      /** Just added here to let you see what the agent is doing, otherwise he will be too quick */
      try {
        this.myAgent.doWait(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // 1) remove the current node from openlist and add it to closedNodes.
      this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

      // 2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
      String nextNodeId = null;
      Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = lobs.iterator();
      while (iter.hasNext()) {
        Location accessibleNode = iter.next().getLeft();
        boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());
        // the node may exist, but not necessarily the edge
        if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
          this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
          if (nextNodeId == null && isNewNode) nextNodeId = accessibleNode.getLocationId();
        }
      }

      // 3) while openNodes is not empty, continues.
      if (!this.myMap.hasOpenNode()) {
        // Explo finished
        finished = true;
        System.out.println(
            this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
      } else {
        // 4) select next move.
        // 4.1 If there exist one open node directly reachable, go for it,
        //	 otherwise choose one from the openNode list, compute the shortestPath and go for it
        if (nextNodeId == null) {
          // no directly accessible openNode
          // chose one, compute the path and take the first step.
          nextNodeId =
              this.myMap
                  .getShortestPathToClosestOpenNode(myPosition.getLocationId())
                  .get(0); // getShortestPath(myPosition,this.openNodes.get(0)).get(0);
          // System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"|
          // nextNode: "+nextNode);
        } else {
          // System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list=
          // "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
        }

        // 5) At each time step, the agent check if he received a graph from a teammate.
        // If it was written properly, this sharing action should be in a dedicated behaviour set.
        MessageTemplate msgTemplate =
            MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
        if (msgReceived != null) {
          SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
          try {
            sgreceived =
                (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
          } catch (UnreadableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          this.myMap.mergeMap(sgreceived);
        }

        ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
      }
    }
  }

  @Override
  public boolean done() {
    return finished;
  }
}
