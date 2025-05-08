package eu.su.mas.dedaleEtu.princ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.io.IOException;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;

public class Utils {
  public static ACLMessage createACLMessage(
      Agent author,
      String protocol,
      AID receiver,
      Serializable content) {
    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    message.setProtocol(protocol);
    message.setSender(author.getAID());
    if (receiver != null) {
      message.addReceiver(receiver);
    } else {
      List<AID> agents = Utils.getOtherAgents(author);
      for (AID agent : agents) {
        message.addReceiver(agent);
      }
    }

    try {
      message.setContentObject(content);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return message;
  }

  public static List<AID> getOtherAgents(Agent agent) {
    try {
      SearchConstraints constraints = new SearchConstraints();
      constraints.setMaxResults(Long.valueOf(-1));
      AMSAgentDescription[] catalog = AMSService.search(agent, new AMSAgentDescription(), constraints);
      AID selfID = agent.getAID();

      return Arrays.stream(catalog)
          .map(AMSAgentDescription::getName)
          .filter(aid -> !aid.equals(selfID))
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.out.println("Problem searching AMS");
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public static float lerp(float a, float b, float t) {
    return (1 - t) * a + t * b;
  }

  public static void waitFor(Agent agent, long millseconds) {
    try {
      agent.doWait(millseconds);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }
}
