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

public class ComputeClosestTreasureBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 1231959282640838272L;

    private boolean initialized = false;

    private Knowledge knowledge;
    private int exitValue;

    public ComputeClosestTreasureBehaviour (Agent myagent, Knowledge knowledge){
        super(myagent);
        this.knowledge = knowledge;
    }

    private void initialize(){
        this.knowledge.setGoal("TREASURE");
        this.initialized = true;
    }

    @Override
    public void action() {
        if (!initialized){
            initialize();
            this.exitValue = 1;
        }
    }

    @Override
    public int onEnd() {
        this.initialized = false;
        return this.exitValue;
    }
}