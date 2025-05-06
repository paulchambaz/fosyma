package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.core.Agent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.princ.Utils;

public class SetMeetingBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -537837587358659414L;

  private int exitValue = 0;

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "Set Meeting Point";

  public SetMeetingBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Set meeting point");

    Communication comms = brain.mind.getCommunication();

    if (comms.shouldSpeak()) {
        String meetingPoint = brain.entities.getMyself().getMeetingPoint();
        if (meetingPoint ==  null){
            String currentPosition = brain.entities.getPosition();
            meetingPoint = Computes.computeMyMeetingPoint(brain.map, 2.0, 1.5, currentPosition);
        }
        if (meetingPoint == null) {
            return;
        }
        brain.entities.getMyself().setMeetingPoint(meetingPoint);

        ACLMessage message = Utils.createACLMessage(
            this.myAgent, PROTOCOL_NAME, comms.getFriend(), meetingPoint);
        ((AbstractDedaleAgent) this.myAgent).sendMessage(message);

    } else {
        MessageTemplate filter = MessageTemplate.and(
            MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
            MessageTemplate.MatchSender(comms.getFriend()));

        ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

        if (message == null) {
            return;
        }

        String meetingPoint = null;
        try {
            meetingPoint = (String) message.getContentObject();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        brain.entities.updateAgentMeetingPoint(comms.getFriend().getLocalName(), meetingPoint);
    }
}

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
