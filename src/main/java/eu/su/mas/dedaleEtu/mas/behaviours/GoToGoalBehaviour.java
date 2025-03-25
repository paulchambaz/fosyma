package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.Memory;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import org.graphstream.algorithm.Dijkstra;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.HashSet;
import jade.core.AID;
import jade.core.Agent;
import java.io.IOException;
import java.util.ArrayList;


import eu.su.mas.dedaleEtu.princ.Utils;

// GoToGoalBehaviour will engage after the exploration of the graph is over.
// This behaviour will direct the agent to the closest treasures depending on its memory.
// TODO : The route to the path is calculated with a Dijkstra algorithm but this will be fixed in the future to a more efficient approach.
public class GoToGoalBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 1233959882640838272L;

    private boolean finished = false;

    private Knowledge knowledge;

    public GoToGoalBehaviour (Agent myagent, Knowledge knowledge){
        super(myagent);
        this.knowledge = knowledge;
    }

    @Override
    public void action() {
        // We calculate the closest treasure to go to and remember the route to go to it
        Map<String, TreasureData> treasures = knowledge.getTreasures();
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        String idFrom = myPosition.getLocationId();
        int closestPathLength = 0;

        TreasureData closestTresor;
        List<String> closestPath = new ArrayList<String>();

        for (Map.Entry<String, TreasureData> entry : treasures.entrySet()) {
            TreasureData tresor = entry.getValue();
            String idTo = tresor.getNodeId();

            List<String> path = knowledge.getShortestPath(idFrom, idTo);
        
            int pathLength = path.size();
            if ((closestPathLength == 0) || (pathLength < closestPathLength)){ // We know this treasure is closer
                closestPath.addAll(path);
                closestPathLength = pathLength;
                closestTresor = tresor;
            }
        }

        // Here we go to the closest treasure found
        for (String nodeId : closestPath){
            try{
                ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nodeId));
            }
            catch (Exception e) {
                this.knowledge.bumpBlockCounter();
            }
        }

        // Then we walk back to the silo
        String siloPosition = knowledge.getSilo().getPosition();
        myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        idFrom = myPosition.getLocationId();
        List<String> pathToSilo = knowledge.getShortestPath(idFrom, siloPosition);

        // Here we go to the silo
        for (String nodeId : pathToSilo){
            try{
                ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nodeId));
            }
            catch (Exception e){
                this.knowledge.bumpBlockCounter();
            }
        }

    }

    @Override
    public boolean done() {
        return finished;
    }
}
