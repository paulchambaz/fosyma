package eu.su.mas.dedaleEtu.mas.knowledge;

// MapAttribute represents the possible states of a node in the map
// - agent: Node is currently occupied by an agent
// - open: Node is discovered but not fully explored
// - closed: Node is fully explored with no further actions needed
public enum MapAttribute {
  agent,
  open,
  closed;
}
