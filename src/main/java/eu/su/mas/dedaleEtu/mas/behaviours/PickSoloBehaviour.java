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

public class PickSoloBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 1231959282640838272L;

    private int exitValue = 0;

    private Knowledge knowledge;

    public PickSoloBehaviour (Agent myagent, Knowledge knowledge){
        super(myagent);
        this.knowledge = knowledge;
    }

    @Override
    public void action() {
        int picked = ((AbstractDedaleAgent) this.myAgent).pick();
        System.out.println(this.myAgent.getLocalName() + " picked " + picked);

        // Check if we still have room in the backpack
        // TODO : for now we consider we have to go to the silo if we have less than 3 spaces for an item but in
        // the future, we could implement a Desire to go back to the silo
        for (Couple<Observation,Integer> freeSpace : ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace()){
            if (freeSpace.getRight() < 3){

                String goal = this.knowledge.getSiloPosition();
                if (goal == null) {
                    // TODO: start LA RONDE
                    System.out.println(this.myAgent.getLocalName() + " wanted to go to a null node");
                    this.exitValue = 2;
                    return;
                }

                this.exitValue = 1;
                this.knowledge.setGoal(goal);
                System.out.println(this.myAgent.getLocalName() + " wants to go to " + goal);
            }
        }
    }

    @Override
    public int onEnd() {
        return this.exitValue;
    }
}