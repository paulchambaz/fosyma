package eu.su.mas.dedaleEtu.princ;

import jade.core.Agent;
import jade.lang.acl.MessageTemplate;

public class Protocols {
  public static Communication handshake(Agent agent, int timeout, String protocol) {
    // first, we test if there is already a handshake sent our way

    MessageTemplate filter = MessageTemplate.and(
      MessageTemplate.MatchPerformative(ACLMessage.INFORM),
      MessageTemplate.MatchProtocol("handshake")
    );

    ACLMessage response = agent.receive(filter);

    if (response == null) {
      // if we have not received any answer then we will try to reach some
      // agent, then await for a response

      ACLMessage bottleToSea = Utils.createACLMessage(
        agent, "handshake", null, protocol
      );
      ((AbstractDedaleAgent) agent).sendMessage(bottleToSea);

      response = agent.blockingReceive(filter, timeout);

      if (response == null) {
        // if we have not received a message either from the initial lookup or
        // from the bottle to the sea, then it means there is not one reachable
        // and the handshake has failed
        return null;
      }
    }

    // but in the case we have gotten a response, either from the initial call
    // or from the bottle to the sea, then we should get information about that
    // content
    String friend = response.getSender();
    String friendProtocol = (String) response.getContent();

    boolean chooses = agent.getLocalName().compareTo(friend) <= 0;

    // if the response is send to me, then it means it was a response from the friend
    // if the response is not then it means it was the initial broadcast
    String sendto = response.getReceiver();
    if (sendto != null) {
      return new Communication(friend, friendProtocol, chooses);
    }

    // we then decide which protocol to use - by convention, we will always use
    // the protocol of the agent with the lowest name in the topological order
    // TODO: in the future it is coherent to send both a protocol and a priority
    String usedProtocol = (chooses) ? protocol : friendProtocol;

    // finally, we sent to the friend that we have gotten what protocol we will
    // actually use for the communication
    ACLMessage message = Utils.createACLMessage(
      agent, "handshake", friend, usedProtocol
    );
    ((AbstractDedaleAgent) agent).sendMessage(message);


    // cleanup in case of asynchronicity - because we can be already certain
    // and both receive extra messages, which are useless and should not
    // perturb future communications
    MessageTemplate cleanupFilter = MessageTemplate.and(
      filter,
      MessageTemplate.MatchSender(friend),
    );
    do {
      response = agent.receive(cleanupFilter);
    } while (response != null);

    // therefore we start the protocol with awaiting a response from sender
    return new Communication(friend, usedProtocol, chooses);
  }
}
