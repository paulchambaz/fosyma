package eu.su.mas.dedaleEtu.princ;

import java.util.List;
import java.io.Serializable;
import java.io.IOException;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.Agent;

public class Utils {
  public static ACLMessage createACLMessage(
    Agent author,
    String protocol,
    List<String> receivers,
    Serializable content
  ) {
    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    message.setProtocol(protocol);
    message.setSender(author.getAID());
    for (String agentName : receivers) {
      message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
    }

    try {
      message.setContentObject(content);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return message;
  }
}

