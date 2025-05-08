# Multi-Agent Treasure Hunt System

A cooperative multi-agent system for exploration and treasure collection in unknown environments using JADE platform, developed for the "Fondement des Systèmes Multi-Agents" (FoSyMa) course at Sorbonne University, Master 1 ANDROIDE.

## About the Project

This project implements a cooperative multi-agent system for exploring unknown environments and collecting resources efficiently. Inspired by the classic "Hunt the Wumpus" game, it tackles fundamental challenges in multi-agent systems:

- Exploration of unknown topologies through frontier-based approaches
- Communication under proximity constraints
- Coordination for tasks requiring multiple agents
- Resource collection and management in dynamic environments
- Handling adversarial entities (Golems)

The implementation uses a sophisticated Finite State Machine (FSM) architecture to manage agent behaviors, with separate strategies for exploration, collection, and coordination roles.

## Key Features

- **Modular Brain Architecture**:

  - AgentMind: Decision-making, behavioral state management
  - WorldMap: Topological knowledge representation, path planning
  - EntityTracker: Entity monitoring with aging mechanism for uncertainty

- **Robust Communication**:

  - Three-phase handshake protocol ensuring reliable connections
  - Incremental knowledge sharing to minimize communication overhead
  - Priority-based message handling

- **Advanced Algorithms**:

  - Min-Max Regret for balanced multi-criteria decision making
  - Adaptive deadlock resolution with expanding search radius
  - Leader-follower coordination for chest opening

- **Specialized Agent Types**:

  - Collector agents (59-state FSM) for resource management
  - Explorer agents (26-state FSM) for efficient mapping
  - Silo agents (18-state FSM) for resource storage

## Usage

To run the program:

```sh
mvn -q compile exec:java -Dexec.mainClass="eu.su.mas.dedaleEtu.princ.Principal" -Dexec.args="{{ ARGS }}"
```

Or, if you have `just`:

```sh
just run
```

## Features

- **Exploration**: Frontier-based approach with probabilistic territory allocation
- **Communication**: Three-phase handshake protocol with priority management
- **Coordination**: Leader-follower dynamics with waypoint guidance
- **Collection**: Multi-criteria resource selection and backpack management

## Authors

- [Paul Chambaz](https://www.linkedin.com/in/paul-chambaz-17235a158/)
- [William Sardon](https://github.com/williamsardon)

## Acknowledgements

This project was developed for the "Fondement des Systèmes Multi-Agents" course at Sorbonne University, based on the Dedale platform created for teaching and research purposes.

## License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.
