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

public class DropOffBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 1231959282640838272L;

    private Knowledge knowledge;
    private int exitValue;

    public DropOffBehaviour (Agent myagent, Knowledge knowledge){
        super(myagent);
        this.knowledge = knowledge;
    }

    @Override
    public void action() {
        ((AbstractDedaleAgent) this.myAgent).emptyMyBackPack(this.myAgent.getLocalName());
        System.out.println(this.myAgent.getLocalName() + " dropped off their backpack into the silo");
    }
}