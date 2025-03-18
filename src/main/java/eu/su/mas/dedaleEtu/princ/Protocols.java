package eu.su.mas.dedaleEtu.princ;

import jade.core.Agent;
import jade.lang.acl.MessageTemplate;

public class Protocols {
  public static String handshake(Agent agent, int timeout, String protocol) {

    // first, we test if there is already a handshake sent our way

    MessageTemplate filter = MessageTemplate.and(
      MessageTemplate.MatchPerformative(ACLMessage.INFORM),
      MessageTemplate.MatchProtocol("handshake")
    );

    ACLMessage response = agent.blockingReceive(filter, timeout);

    if (response != null) {
      // if there is then agent A knows we have started communication but agent
      // B does not and both agents dont know which protocol they will use
      // therefore, we will need to send a response to the other agent

      // we still need to decide which protocol to use, in order to break
      // symetry, we will always accept the protocol of the agent that has the
      // first name in the lexicographical order, given that it is a string

      String friend = response.getSender();
      String friendProtocol = (String) response.getContent();

      String usedProtocol = (friend.compareTo(agent.getLocalName()) ? friendProtocol : protocol;

      ACLMessage answer = Utils.createACLMessage(
        agent, "handshake", friend, usedProtocol,
      );

      return response.getSender();
    }

    // if we have not received any answer then we will try to reach 



  }
}
