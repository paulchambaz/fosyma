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
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.Iterator;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
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
public class GoToGoalBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 1233959882640838272L;

    private boolean initialized = false;

    private Knowledge knowledge;
    private Deque<String> pathToGoal;

    private int exitValue;

    public GoToGoalBehaviour (Agent myagent, Knowledge knowledge){
        super(myagent);
        this.knowledge = knowledge;
    }

    private void initialize(){
        System.out.println("GOING TO GOAL " + myAgent.getLocalName());
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();      
        if (myPosition == null) {
            return;
        }
        this.knowledge.updateAgentPosition(myPosition.getLocationId());                            
        this.knowledge.updateGoal(myPosition.getLocationId());

        this.pathToGoal = this.knowledge.getGoalPath();
        System.out.println(this.pathToGoal);
        this.initialized = true;
    }

    @Override
    public void action() {
        if (!initialized){
            initialize();
        }
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition == null) {
            return;
        }
        this.knowledge.updateAgentPosition(myPosition.getLocationId());

        try {
            this.myAgent.doWait(500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pathToGoal.isEmpty()){
            // here we normally found the goal, we can check if it's still here in case the golem moved it and open it
            List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();
            Observation myTreasureType = ((AbstractDedaleAgent) this.myAgent).getMyTreasureType();

            for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
                for (Couple<Observation, String> observation : entry.getRight()) {
                    Observation observeKind = observation.getLeft();
                    String observed = observation.getRight();

                    switch (observeKind) {
                        case GOLD:
                            this.exitValue = 1;
                            break;
                        case AGENTNAME:
                            if (observed.startsWith("Silo"))
                                this.exitValue = 2;
                            break;
                        default:
                            assert false : "Unhandled observation type: " + observeKind;
                    }
                }
            }
        }

        try{
            ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(pathToGoal.removeFirst()));
        }
        catch (Exception e) {
            this.knowledge.bumpBlockCounter();
            // we are stuck at a point, we can try and recalculate a dijkstra here to the treasure
            myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            if (myPosition == null) {
                return;
            }
            this.knowledge.updateAgentPosition(myPosition.getLocationId());   
            this.knowledge.updateGoal(myPosition.getLocationId());
        }
    }

    @Override
    public int onEnd() {
        this.initialized = false;
        return this.exitValue;
    }
}
