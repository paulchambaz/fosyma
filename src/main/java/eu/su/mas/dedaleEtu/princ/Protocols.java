package eu.su.mas.dedaleEtu.princ;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import java.io.Serializable;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;

public class Protocols {
  private static String PROTOCOL_HANDSHAKE = "handshake";

  public static Communication exclusiveHandshake(Agent agent, Brain brain, int timeout, String protocol,
      int priority, String target) {
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "2");
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "1");

    MessageTemplate filter;
    ACLMessage response;
    AID friendA, friendB, friend;
    HandshakeData protocolA, protocolB, usedProtocol;
    boolean speaker;

    filter = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "0"));

    response = agent.receive(filter);

    if (response != null) {
      friendA = response.getSender();
      // Check if the sender matches the target
      if (!friendA.getLocalName().equals(target)) {
        return null;
      }

      try {
        protocolA = (HandshakeData) response.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }

      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "1", friendA, new HandshakeData(protocol, priority)));

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
        // Check if the sender matches the target
        if (!friendB.getLocalName().equals(target)) {
          return null;
        }

        try {
          protocolB = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
            protocolB.priority);
        friend = withA ? friendA : friendB;

        speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friend, new HandshakeData(protocol, priority)));

        return new Communication(friend, usedProtocol.protocol, speaker);
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
        friendB = response.getSender();
        // Check if the sender matches the target
        if (!friendB.getLocalName().equals(target)) {
          return null;
        }

        try {
          protocolB = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
            protocolB.priority);
        friend = withA ? friendA : friendB;

        speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

        return new Communication(friend, usedProtocol.protocol, speaker);
      }
    }

    if (response == null) {
      if (!brain.mind.isReadyForSocialInteraction()) {
        return null;
      }

      AID targetAID = null;
      List<AID> otherAgents = Utils.getOtherAgents(agent);
      for (AID otherAgent : otherAgents) {
        if (otherAgent.getLocalName().equals(target)) {
          targetAID = otherAgent;
        }
      }
      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "0", targetAID, new HandshakeData(protocol, priority)));
      // after we send a bottle to the sea, we always wait at least one step
      // before we talk again
      brain.mind.initiateSocialCooldown();

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
        // Check if the sender matches the target
        if (!friendA.getLocalName().equals(target)) {
          return null;
        }

        try {
          protocolA = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, new HandshakeData(protocol, priority)));

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
          // Check if the sender matches the target
          if (!friendB.getLocalName().equals(target)) {
            return null;
          }

          try {
            protocolB = (HandshakeData) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
              protocolB.priority);
          friend = withA ? friendA : friendB;

          speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
          usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

          ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
              agent, PROTOCOL_HANDSHAKE + "2", friend, new HandshakeData(protocol, priority)));

          return new Communication(friend, usedProtocol.protocol, speaker);
        }

        if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
          friendB = response.getSender();
          // Check if the sender matches the target
          if (!friendB.getLocalName().equals(target)) {
            return null;
          }

          try {
            protocolB = (HandshakeData) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
              protocolB.priority);
          friend = withA ? friendA : friendB;

          speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
          usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

          return new Communication(friend, usedProtocol.protocol, speaker);
        }
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "1")) {
        friendA = response.getSender();
        // Check if the sender matches the target
        if (!friendA.getLocalName().equals(target)) {
          return null;
        }

        try {
          protocolA = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        speaker = shouldSpeak(agent, friendA, priority, protocolA.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : protocolA;

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, usedProtocol));

        return new Communication(friendA, usedProtocol.protocol, speaker);
      }
    }

    return null;
  }

  public static Communication handshake(Agent agent, Brain brain, int timeout, String protocol, int priority) {
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "2");
    emptyMessageCue(agent, PROTOCOL_HANDSHAKE + "1");

    MessageTemplate filter;
    ACLMessage response;
    AID friendA, friendB, friend;
    HandshakeData protocolA, protocolB, usedProtocol;
    boolean speaker;

    filter = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(PROTOCOL_HANDSHAKE + "0"));

    response = agent.receive(filter);

    if (response != null) {
      friendA = response.getSender();
      try {
        protocolA = (HandshakeData) response.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }

      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "1", friendA, new HandshakeData(protocol, priority)));

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
          protocolB = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
            protocolB.priority);
        friend = withA ? friendA : friendB;

        speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friend, new HandshakeData(protocol, priority)));

        return new Communication(friend, usedProtocol.protocol, speaker);
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
        friendB = response.getSender();
        try {
          protocolB = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
            protocolB.priority);
        friend = withA ? friendA : friendB;

        speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

        return new Communication(friend, usedProtocol.protocol, speaker);
      }
    }

    if (response == null) {
      if (!brain.mind.isReadyForSocialInteraction()) {
        return null;
      }

      ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
          agent, PROTOCOL_HANDSHAKE + "0", null, new HandshakeData(protocol, priority)));
      // after we send a bottle to the sea, we always wait at least one step
      // before we talk again
      brain.mind.initiateSocialCooldown();

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
          protocolA = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, new HandshakeData(protocol, priority)));

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
            protocolB = (HandshakeData) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
              protocolB.priority);
          friend = withA ? friendA : friendB;

          speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
          usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

          ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
              agent, PROTOCOL_HANDSHAKE + "2", friend, new HandshakeData(protocol, priority)));

          return new Communication(friend, usedProtocol.protocol, speaker);
        }

        if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "2")) {
          friendB = response.getSender();
          try {
            protocolB = (HandshakeData) response.getContentObject();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }

          boolean withA = shouldCommunicateWith(agent, friendA, friendB, priority, protocolA.priority,
              protocolB.priority);
          friend = withA ? friendA : friendB;

          speaker = shouldSpeak(agent, friend, priority, withA ? protocolA.priority : protocolB.priority);
          usedProtocol = speaker ? new HandshakeData(protocol, priority) : (withA ? protocolA : protocolB);

          return new Communication(friend, usedProtocol.protocol, speaker);
        }
      }

      if (response.getProtocol().equals(PROTOCOL_HANDSHAKE + "1")) {
        friendA = response.getSender();
        try {
          protocolA = (HandshakeData) response.getContentObject();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        speaker = shouldSpeak(agent, friendA, priority, protocolA.priority);
        usedProtocol = speaker ? new HandshakeData(protocol, priority) : protocolA;

        ((AbstractDedaleAgent) agent).sendMessage(Utils.createACLMessage(
            agent, PROTOCOL_HANDSHAKE + "2", friendA, usedProtocol));

        return new Communication(friendA, usedProtocol.protocol, speaker);
      }
    }

    return null;
  }

  private static boolean shouldCommunicateWith(Agent agent, AID friendA, AID friendB, int myPriority, int priorityA,
      int priorityB) {
    if (priorityA != priorityB) {
      return priorityA > priorityB;
    }

    return friendA.getLocalName().compareTo(friendB.getLocalName()) <= 0;
  }

  private static boolean shouldSpeak(Agent agent, AID friend, int myPriority, int friendPriority) {
    if (myPriority != friendPriority) {
      return myPriority > friendPriority;
    }

    return agent.getLocalName().compareTo(friend.getLocalName()) <= 0;
  }

  private static class HandshakeData implements Serializable {
    private static final long serialVersionUID = 1L;
    public String protocol;
    public int priority;

    public HandshakeData(String protocol, int priority) {
      this.protocol = protocol;
      this.priority = priority;
    }
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
