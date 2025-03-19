package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.SerializableKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.Memory;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Protocols;
import eu.su.mas.dedaleEtu.princ.Communication;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.lang.acl.ACLMessage;
import java.util.List;

public class ShareMapBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = -568863390879327961L;
  private boolean finished = false;

  private Knowledge knowledge;
  private List<String> receiverAgents;

  public ShareMapBehaviour(Agent agent, Knowledge knowledge, List<String> receiverAgents) {
    super(agent);
    this.knowledge = knowledge;
    this.receiverAgents = receiverAgents;
  }

  @Override
  public void action() {
  }

  @Override
  public boolean done() {
    return finished;
  }
}
