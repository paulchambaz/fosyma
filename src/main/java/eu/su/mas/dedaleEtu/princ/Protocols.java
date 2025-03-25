package eu.su.mas.dedaleEtu.princ;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;

public class Protocols {

  private static String PROTOCOL_HANDSHAKE = "hs";

  public static Communication handshake(Agent agent, Knowledge knowledge, int timeout, String protocol) {
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "2");
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "1");

    MessageTemplate filter;
    ACLMessage response;
    AID friendA, friendB, friend;
    String protocolA, protocolB, usedProtocol;
    boolean speaker;

    filter = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "0"));

    response = agent.receive(filter);

    if (response != null) {
      friendA = response.getSender();
      try {
        protocolA = (String) response.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }

      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "1", friendA, protocol));

      filter = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.or(
              MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "1"),
              MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "2")));

      response = agent.blockingReceive(filter, timeout);

      if (response == null) {
        return null;
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "1")) {
        friendB = response.getSender();
        try {
          protocolB = (String) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = friendA.getLocalName().compareTo(friendB.getLocalName()) <= 0;
        friend = withA ? friendA : friendB;

        speaker = agent.getLocalName().compareTo(friend.getLocalName()) <= 0;
        usedProtocol = speaker ? protocol : (withA ? protocolA : protocolB);

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friend, protocol));

        return new Communication(friend, usedProtocol, speaker);
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
        friendB = response.getSender();
        try {
          protocolB = (String) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = friendA.getLocalName().compareTo(friendB.getLocalName()) <= 0;
        friend = withA ? friendA : friendB;

        speaker = agent.getLocalName().compareTo(friend.getLocalName()) <= 0;
        usedProtocol = speaker ? protocol : (withA ? protocolA : protocolB);

        return new Communication(friend, usedProtocol, speaker);
      }
    }

    if (response == null) {
      if (!knowledge.introvertCanTalk()) {
        return null;
      }

      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "0", null, protocol));
      // after we send a bottle to the sea, we always wait at least one step
      // before we talk again
      knowledge.introvertSoftReset();

      filter = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.or(
              MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "0"),
              MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "1")));

      response = agent.blockingReceive(filter, timeout);

      if (response == null) {
        return null;
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "0")) {
        friendA = response.getSender();
        try {
          protocolA = (String) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, protocol));

        filter = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.or(
                MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "1"),
                MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "2")));

        response = agent.blockingReceive(filter, timeout);

        if (response == null) {
          return null;
        }

        if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "1")) {
          friendB = response.getSender();
          try {
            protocolB = (String) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = friendA.getLocalName().compareTo(friendB.getLocalName()) <= 0;
          friend = withA ? friendA : friendB;

          speaker = agent.getLocalName().compareTo(friend.getLocalName()) <= 0;
          usedProtocol = speaker ? protocol : (withA ? protocolA : protocolB);

          ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
              agent, PROTOCOL_HANDSHAKE + "2", friend, protocol));

          return new Communication(friend, usedProtocol, speaker);
        }

        if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
          friendB = response.getSender();
          try {
            protocolB = (String) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = friendA.getLocalName().compareTo(friendB.getLocalName()) <= 0;
          friend = withA ? friendA : friendB;

          speaker = agent.getLocalName().compareTo(friend.getLocalName()) <= 0;
          usedProtocol = speaker ? protocol : (withA ? protocolA : protocolB);

          return new Communication(friend, usedProtocol, speaker);
        }
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "1")) {
        friendA = response.getSender();
        try {
          protocolA = (String) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        speaker = agent.getLocalName().compareTo(friendA.getLocalName()) <= 0;
        usedProtocol = speaker ? protocol : protocolA;

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, usedProtocol));

        return new Communication(friendA, usedProtocol, speaker);
      }
    }

    return null;
  }

  private static void emptyMessageCue(Agent agent, String protocol) {
    MessageTemplate filter = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(protocol));

    ACLMessage response;
    do {
      response = agent.receive(filter);
    } while (response != null);
  }
}
