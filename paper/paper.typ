#import "template.typ": *
#import "@preview/cetz:0.3.1": canvas, draw

#show: report.with(
  title: [ Chasse au trésor pour système multi-agents ],
  course: [ Projet Fosyma -- Groupe 12 ],
  authors: ("Paul Chambaz", "William Sardon"),
  university: [ Sorbonne Université ],
  reference: [ #smallcaps[Ai2d] M1 ],
  nb-columns: 1,
)

#align(center)[
  Le code est disponible sur GitHub en public : #link("https://github.com/paulchambaz/fosyma").
]

== Introduction

Ce rapport présente notre implémentation d'un système multi-agents pour la recherche coopérative de trésors dans un environnement inconnu, basée sur la Plateforme Dédale et Jade. Le projet s'inspire du jeu "Hunt the Wumpus", mais dans une variante où plusieurs agents collaborent pour cartographier l'environnement et récupérer des ressources.

L'environnement se compose de pièces interconnectées formant un graphe, initialement inconnu des agents. Ces derniers commencent leur exploration à partir de positions distinctes et doivent progressivement construire une représentation mentale de la topologie. La tâche est complexifiée par plusieurs contraintes: les agents possèdent une perception limitée de leur position actuelle et des zones adjacentes, leur capacité de transport est restreinte, et certains trésors sont enfermés dans des coffres nécessitant des compétences spécifiques pour être ouverts.

La communication entre agents constitue un aspect important du projet. Les agents peuvent échanger des informations uniquement lorsqu'ils se trouvent à proximité les uns des autres, dans un rayon de communication défini. Cette contrainte impose de développer des stratégies de rencontre planifiées et de partager efficacement les connaissances acquises.

La présence d'agents adverses (Golems) ajoute une dimension dynamique au problème. Ils se déplacent dans l'environnement et peuvent déplacer les trésors ou refermer des coffres précédemment ouverts, obligeant les agents à adapter leur stratégie en fonction des changements observés.

Notre approche repose sur une architecture qui utilise un automate à états finis (FSM) comprenant des états organisés en groupes fonctionnels: exploration, collecte, dépôt des ressources, coordination pour l'ouverture des coffres, et comportements spécifiques pour les rôles de leader et suiveur. Cette structure modulaire permet une séparation des responsabilités tout en facilitant la gestion des transitions entre différents comportements selon le contexte.

== Architecture
// architecture générale du code et diagrammes UML (Structure , Agents et comportements)

=== Composants de la représentation de connaissances

Notre architecture organise la représentation des connaissances de l'agent autour d'une structure nommée "Brain". Cette structure comporte trois composants principaux qui fonctionnent ensemble pour maintenir une représentation cohérente de l'environnement.

==== AgentMind
L'AgentMind gère l'état comportemental de l'agent et sa prise de décision. Ce composant maintient des données sur l'état courant, incluant le comportement actif, les nœuds cibles et les priorités d'action.

Un mécanisme central de l'AgentMind est la gestion des transitions entre les comportements d'exploration et de collecte. Ces transitions s'effectuent graduellement grâce à un système de pondération qui adapte les priorités selon les conditions environnementales. L'état de l'agent n'est pas modifié de manière binaire mais par interpolation linéaire entre différentes valeurs de priorité.

L'AgentMind implémente également un mécanisme de "social cooldown" qui régule la fréquence de l'initiation de communications avec les autres agents. Un compteur est incrémenté après chaque déplacement et réinitialisé lors des communications, assurant ainsi une période minimale entre les tentatives de communication. Un compteur de blocage est de même utilisé pour garder en mémoire les situations où l'agent n'a pas pu agir comme il le veut. Cette information est utilisée pour résoudre les problèmes d'interblocages.

==== WorldMap
Le WorldMap représente la connaissance topologique que l'agent a de l'environnement. Ce composant maintient un graphe où chaque nœud correspond à une position et chaque arête à un passage entre des positions adjacentes.

Le WorldMap attribue à chaque nœud un état: ouvert ou fermé. Les nœuds ouverts indiquent des zones identifiées mais non entièrement explorées, tandis que les nœuds fermés ont  complètement été examinés. Cette distinction sert à identifier les frontières d'exploration pour orienter les déplacements futurs.

Ce composant fournit des fonctionnalités pour le calcul de chemins optimaux en utilisant l'algorithme de Dijkstra. Il permet également de créer des sous-graphes navigables qui excluent les positions occupées par d'autres entités. La sérialisation du graphe facilite le transfert de données entre agents, en transmettant uniquement les portions de carte inconnues du destinataire.

==== EntityTracker
L'EntityTracker monitore les entités dynamiques présentes dans l'environnement. Il enregistre et met à jour les informations concernant les autres agents, les silos, les golems et les trésors.

Ce composant garde en mémoire les propriétés des trésors découverts: type, quantité, état du verrou et force requise pour le transport. Pour les agents, il enregistre leur position et leurs capacités. Le mécanisme de vieillissement des connaissances incrémente un compteur pour chaque entité non observée directement. Lors du partage de connaissance, l'âge de la connaissance est utilisé pour choisir quelle information est à suivre.

L'EntityTracker conserve également un historique des points partagés avec les agents. Cette information est utilisé lors du partage de carte pour réduire les communications aux seuls nœuds qui n'ont pas encore été partagés entre les agents.

La fusion des connaissances reçues est gérée par un ensemble de règles de résolution de conflits. Lorsque des informations contradictoires sont reçues, comme différentes propriétés pour un même trésor, le système privilégie l'information la plus récente en comparant les compteurs d'actualité. Pour les données topologiques, les nœuds fermés sont prioritaires sur les nœuds ouverts, reflétant une exploration plus complète.

=== Architecture avec automate à états finis (FSM)

Notre système repose sur une architecture d'automates finis qui permet de modéliser le comportement des agents de manière modulaire. Le choix d'implémenter une machine à états finis a été motivé par la nécessité de gérer la complexité des comportements adaptatifs tout en maintenant une structure claire et déterministe. Le FSM des agents d'exploration est représenté dans les figures suivantes.



==== Organisation hiérarchique des états
L'automate est structuré en trois niveaux hiérarchiques correspondant à différents niveaux d'abstraction du comportement des agents:

1. États primaires: Ils définissent les objectifs stratégiques de haut niveau de l'agent. Ces états déterminent le but global poursuivi à un moment donné.
  - EXPLORE: Dédié à la cartographie systématique de l'environnement inconnu
  - COLLECT: Centré sur l'acquisition des ressources accessibles
  - DROP: Gérant le retour au silo et le dépôt des ressources accumulées
  - CHEST: Orchestrant la coordination nécessaire pour l'ouverture des coffres verrouillés
  - LEADER/FOLLOWER: Déterminant les rôles spécifiques lors de la coordination inter-agents
  - END: Contrôlant le comportement de fin de mission quand la carte est complète


#figure(caption: [Représentation de l'automate des comportement d'exploration], canvas(length: 1.8cm, {
  node((-6, 0), "INIT", kind: "rect", ratio: 2);
  node((-6, -1), "EXPLORE", kind: "rect", ratio: 3);
  node((-3, -2), "EXPLORE_GOTO", kind: "rect", ratio: 5);
  node((-4, -0.5), "EXPLORE_DEADLOCK", kind: "rect", ratio: 6);
  node((0, 0), "EXPLORE_COMM", kind: "rect", ratio: 5);
  node((1, -1), "EXPLORE_COMM_SHARE", kind: "rect", ratio: 7);
  node((1, -1.8), "EXPLORE_COMM_MEETING", kind: "rect", ratio: 8);
  node((1, -2.6), "EXPLORE_COMM_PLAN", kind: "rect", ratio: 7);
  // reliés au graphe extérieur, changé de couleur ?
  node((1, 1), "CHEST_NEGOTIATION", kind: "rect", ratio: 6); 
  node((-6, -3), "COLLECT", kind: "rect", ratio: 3);

  arc("INIT", "EXPLORE", alpha: 0.2);
  arc("EXPLORE", "EXPLORE_GOTO", alpha: 0.2);
  arc("EXPLORE", "COLLECT", alpha: 0.2, value: [1]);
  arc("EXPLORE_GOTO", "EXPLORE_COMM", alpha: 0.2);
  arc("EXPLORE_GOTO", "EXPLORE", alpha: 0.2, value: [1]);
  arc("EXPLORE_GOTO", "EXPLORE_DEADLOCK", alpha: 0.2, value: [2]);
  arc("EXPLORE_COMM", "EXPLORE_GOTO", alpha: 0.2);
  arc("EXPLORE_COMM", "EXPLORE_COMM_SHARE", alpha: -0.2, value: [1]);
  arc("EXPLORE_COMM", "CHEST_NEGOTIATION", alpha: 0.2, value: [2]);
  arc("EXPLORE_COMM", "EXPLORE_DEADLOCK", alpha: 0.2, value: [3]);
  arc("EXPLORE_COMM_SHARE", "EXPLORE_COMM_MEETING", alpha: 0.2);
  arc("EXPLORE_COMM_MEETING", "EXPLORE_COMM_PLAN", alpha: 0.2);
  arc("EXPLORE_COMM_PLAN", "EXPLORE_GOTO", alpha: -0.2);
  arc("EXPLORE_DEADLOCK", "EXPLORE_GOTO", alpha: 0.2);
}))
  
2. États secondaires: Ils implémentent les mécanismes tactiques nécessaires à la réalisation des objectifs primaires.
  - GOTO: Gère la navigation vers un objectif spécifique, avec suivi de chemin et détection d'obstacles
  - COMM: Établit et maintient les communications avec les autres agents selon différents protocoles
  - DEADLOCK: Détecte et résout les situations de blocage par recherche de chemins alternatifs

3. États spécialisés: Ils exécutent des actions spécifiques au niveau le moins général de l'automate.
  - OPENLOCK: Tente d'ouvrir un coffre et évalue les compétences requises
  - PICK: Ramasse une ressource et met à jour l'inventaire de l'agent
  - DROPOFF: Transfère les ressources collectées vers un silo

#figure(caption: [Représentation de l'automate des comportements de collecte -- Partie 1], canvas(length: 1.8cm, {
  node((-0, -6.5), "COLLECT", kind: "rect", ratio: 3);
  node((1.6, -4.5), "COLLECT_GOTO", kind: "rect", ratio: 5);
  node((2.5, -3.2), "COLLECT_DEADLOCK", kind: "rect", ratio: 6);
  node((-1, -3.8), "COLLECT_COMM", kind: "rect", ratio: 5);
  node((-0, -2.7), "COLLECT_COMM_SHARE", kind: "rect", ratio: 7);
  node((2.7, -2.2), "COLLECT_COMM_MEETING", kind: "rect", ratio: 8);
  node((3, -5.5), "COLLECT_OPENLOCK", kind: "rect", ratio: 6);
  node((3, -6.2), "COLLECT_PICK", kind: "rect", ratio: 5);
  node((0, -8.8), "DROP_LOCATE_SILO", kind: "rect", ratio: 6);
  node((3, -8.3), "DROP_GOTO_SILO", kind: "rect", ratio: 5.5);
  node((3, -6.9), "DROP_DEADLOCK", kind: "rect", ratio: 5);
  node((-1, -7.5), "DROP_COMM", kind: "rect", ratio: 4);
  node((-3.5, -8.2), "DROP_COMM_SHARE", kind: "rect", ratio: 6);
  node((-3, -9), "DROP_COMM_MEETING", kind: "rect", ratio: 7);
  node((3, -7.5), "DROP_DROPOFF", kind: "rect", ratio: 5);
  node((-4, -6.5), "CHEST_LOCATE_AGENT", kind: "rect", ratio: 7);
  node((-4, -5.3), "CHEST_GOTO_AGENT", kind: "rect", ratio: 7);
  node((-4.5, -4), "CHEST_DEADLOCK", kind: "rect", ratio: 6);
  node((-4.5, -3), "CHEST_COMM", kind: "rect", ratio: 4);
  node((-1.5, -1.5), "CHEST_COMM_SHARE", kind: "rect", ratio: 6);
  node((-3.5, -0.5), "CHEST_COMM_MEETING", kind: "rect", ratio: 7);
  node((-0.7, -5), "CHEST_INIT_COMM", kind: "rect", ratio: 6);
  node((-4, -1.8), "CHEST_NEGOTIATION", kind: "rect", ratio: 6);
  node((1.7, -1.5), "LEADER_COMPUTE_WAYPOINT", kind: "rect", ratio: 8);
  node((0, -0.4), "LEADER_COMM", kind: "rect", ratio: 5);
  node((0, 0.5), "LEADER_COMM_WAYPOINT", kind: "rect", ratio: 7);
  node((0, 1.3), "LEADER_GOTO", kind: "rect", ratio: 5);
  node((2, 2), "LEADER_DEADLOCK", kind: "rect", ratio: 6);
  node((-2, 2), "LEADER_GOTO_DEADLOCK", kind: "rect", ratio: 7.5);
  node((-2.5, 1.3), "LEADER_RESTORE", kind: "rect", ratio: 5.5);
  node((2.5, 1.3), "LEADER_WAITFOR", kind: "rect", ratio: 6);
  node((3, 0.5), "LEADER_COMM_ARRIVED", kind: "rect", ratio: 7);
  node((3, -0.4), "LEADER_OPENLOCK", kind: "rect", ratio: 6);

  arc("COLLECT", "COLLECT_GOTO", alpha: 0.2);
  arc("COLLECT_GOTO", "COLLECT_COMM", alpha: 0.6);
  arc("COLLECT_GOTO", "COLLECT_OPENLOCK", alpha: 0.2, value: [1]);
  arc("COLLECT_GOTO", "COLLECT_DEADLOCK", alpha: 0.2, value: [2]);
  arc("COLLECT_COMM", "COLLECT_GOTO", alpha: 0.2);
  arc("COLLECT_COMM", "COLLECT_COMM_SHARE", alpha: 0.2, value: [1]);
  arc("COLLECT_COMM", "CHEST_NEGOTIATION", alpha: 0.2, value: [2]);
  arc("COLLECT_COMM", "COLLECT_DEADLOCK", alpha: 0.2, value: [3]);
  arc("COLLECT_COMM_SHARE", "COLLECT_COMM_MEETING", alpha: 0.2);
  arc("COLLECT_COMM_MEETING", "COLLECT_GOTO", alpha: -0.6);
  arc("COLLECT_DEADLOCK", "COLLECT_GOTO", alpha: 0.2);
  arc("COLLECT_OPENLOCK", "COLLECT_PICK", alpha: 0.2);
  arc("COLLECT_OPENLOCK", "CHEST_LOCATE_AGENT", alpha: 0.2, value: [1]);
  arc("COLLECT_PICK", "DROP_LOCATE_SILO", alpha: 0.2);
  arc("DROP_LOCATE_SILO", "DROP_GOTO_SILO", alpha: 0.2);
  arc("DROP_GOTO_SILO", "DROP_COMM", alpha: 0.2);
  arc("DROP_GOTO_SILO", "DROP_LOCATE_SILO", alpha: 0.2, value: [1]);
  arc("DROP_GOTO_SILO", "DROP_DEADLOCK", alpha: 0.2, value: [2]);
  arc("DROP_GOTO_SILO", "DROP_DROPOFF", alpha: 0.2, value: [3]);
  arc("DROP_COMM", "DROP_GOTO_SILO", alpha: 0.2);
  arc("DROP_COMM", "DROP_COMM_SHARE", alpha: 0.2, value: [1]);
  arc("DROP_COMM", "DROP_DEADLOCK", alpha: 0.2, value: [2]);
  arc("DROP_COMM_SHARE", "DROP_COMM_MEETING", alpha: 0.2);
  arc("DROP_COMM_MEETING", "DROP_GOTO_SILO", alpha: 0.3);
  arc("DROP_DEADLOCK", "DROP_GOTO_SILO", alpha: 0.2);
  arc("DROP_DROPOFF", "COLLECT", alpha: 0.2);
  arc("DROP_DROPOFF", "DROP_LOCATE_SILO", alpha: 0.2, value: [1]);
  arc("CHEST_LOCATE_AGENT", "COLLECT", alpha: 0.2);
  arc("CHEST_LOCATE_AGENT", "CHEST_GOTO_AGENT", alpha: 0.2, value: [1]);
  arc("CHEST_LOCATE_AGENT", "DROP_LOCATE_SILO", alpha: 0.2, value: [2]);
  arc("CHEST_GOTO_AGENT", "CHEST_COMM", alpha: 0.2);
  arc("CHEST_GOTO_AGENT", "CHEST_LOCATE_AGENT", alpha: 0.2, value: [1]);
  arc("CHEST_GOTO_AGENT", "CHEST_DEADLOCK", alpha: 0.2, value: [2]);
  arc("CHEST_GOTO_AGENT", "CHEST_DEADLOCK", alpha: 0.2, value: [3]);
  arc("CHEST_INIT_COMM", "CHEST_LOCATE_AGENT", alpha: 0.2);
  arc("CHEST_INIT_COMM", "CHEST_NEGOTIATION", alpha: 0.2, value: [1]);
  arc("CHEST_INIT_COMM", "CHEST_DEADLOCK", alpha: 0.2, value: [2]);
  arc("CHEST_DEADLOCK", "CHEST_GOTO_AGENT", alpha: 0.2);
  arc("CHEST_COMM", "CHEST_GOTO_AGENT", alpha: 0.2);
  arc("CHEST_COMM", "CHEST_COMM_SHARE", alpha: 0.2, value: [1]);
  arc("CHEST_COMM", "CHEST_NEGOTIATION", alpha: 0.2, value: [2]);
  arc("CHEST_COMM", "CHEST_DEADLOCK", alpha: 0.2, value: [3]);
  arc("CHEST_COMM_SHARE", "CHEST_COMM_MEETING", alpha: 0.2);
  arc("CHEST_COMM_MEETING", "CHEST_NEGOTIATION", alpha: 0.2);
  arc("CHEST_NEGOTIATION", "COLLECT", alpha: 0.2);
  arc("CHEST_NEGOTIATION", "LEADER_COMPUTE_WAYPOINT", alpha: 0.2, value: [1]);
  arc("LEADER_COMPUTE_WAYPOINT", "LEADER_COMM", alpha: 0.2);
  arc("LEADER_COMM", "CHEST_LOCATE_AGENT", alpha: 0.2);
  arc("LEADER_COMM", "LEADER_COMM_WAYPOINT", alpha: 0.2, value: [1]);
  arc("LEADER_COMM", "LEADER_DEADLOCK", alpha: 0.2, value: [2]);
  arc("LEADER_COMM_WAYPOINT", "LEADER_GOTO", alpha: 0.2);
  arc("LEADER_GOTO", "LEADER_GOTO", alpha: 0.2);
  arc("LEADER_GOTO", "LEADER_WAITFOR", alpha: 0.2, value: [1]);
  arc("LEADER_GOTO", "LEADER_DEADLOCK", alpha: 0.2, value: [2]);
  arc("LEADER_DEADLOCK", "LEADER_GOTO_DEADLOCK", alpha: 0.2);
  arc("LEADER_GOTO_DEADLOCK", "LEADER_GOTO_DEADLOCK", alpha: 0.2);
  arc("LEADER_GOTO_DEADLOCK", "LEADER_RESTORE", alpha: 0.2, value: [1]);
  arc("LEADER_GOTO_DEADLOCK", "LEADER_DEADLOCK", alpha: 0.2, value: [2]);
  arc("LEADER_RESTORE", "LEADER_GOTO", alpha: 0.2);
  arc("LEADER_WAITFOR", "LEADER_WAITFOR", alpha: 0.2);
  arc("LEADER_WAITFOR", "LEADER_COMM_ARRIVED", alpha: 0.2, value: [1]);
  arc("LEADER_COMM_ARRIVED", "CHEST_LOCATE_AGENT", alpha: 0.2);
  arc("LEADER_COMM_ARRIVED", "LEADER_OPENLOCK", alpha: 0.2, value: [1]);
  arc("LEADER_OPENLOCK", "COLLECT_PICK", alpha: -0.2);

  node((-0, -6.5), "COLLECT", kind: "rect", ratio: 3);
  node((1.6, -4.5), "COLLECT_GOTO", kind: "rect", ratio: 5);
  node((2.5, -3.2), "COLLECT_DEADLOCK", kind: "rect", ratio: 6);
  node((-1, -3.8), "COLLECT_COMM", kind: "rect", ratio: 5);
  node((-0, -2.7), "COLLECT_COMM_SHARE", kind: "rect", ratio: 7);
  node((2.7, -2.2), "COLLECT_COMM_MEETING", kind: "rect", ratio: 8);
  node((3, -5.5), "COLLECT_OPENLOCK", kind: "rect", ratio: 6);
  node((3, -6.2), "COLLECT_PICK", kind: "rect", ratio: 5);
  node((0, -8.8), "DROP_LOCATE_SILO", kind: "rect", ratio: 6);
  node((3, -8.3), "DROP_GOTO_SILO", kind: "rect", ratio: 5.5);
  node((3, -6.9), "DROP_DEADLOCK", kind: "rect", ratio: 5);
  node((-1, -7.5), "DROP_COMM", kind: "rect", ratio: 4);
  node((-3.5, -8.2), "DROP_COMM_SHARE", kind: "rect", ratio: 6);
  node((-3, -9), "DROP_COMM_MEETING", kind: "rect", ratio: 7);
  node((3, -7.5), "DROP_DROPOFF", kind: "rect", ratio: 5);
  node((-4, -6.5), "CHEST_LOCATE_AGENT", kind: "rect", ratio: 7);
  node((-4, -5.3), "CHEST_GOTO_AGENT", kind: "rect", ratio: 7);
  node((-4.5, -4), "CHEST_DEADLOCK", kind: "rect", ratio: 6);
  node((-4.5, -3), "CHEST_COMM", kind: "rect", ratio: 4);
  node((-1.5, -1.5), "CHEST_COMM_SHARE", kind: "rect", ratio: 6);
  node((-3.5, -0.5), "CHEST_COMM_MEETING", kind: "rect", ratio: 7);
  node((-0.7, -5), "CHEST_INIT_COMM", kind: "rect", ratio: 6);
  node((-4, -1.8), "CHEST_NEGOTIATION", kind: "rect", ratio: 6);
  node((1.7, -1.5), "LEADER_COMPUTE_WAYPOINT", kind: "rect", ratio: 8);
  node((0, -0.4), "LEADER_COMM", kind: "rect", ratio: 5);
  node((0, 0.5), "LEADER_COMM_WAYPOINT", kind: "rect", ratio: 7);
  node((0, 1.3), "LEADER_GOTO", kind: "rect", ratio: 5);
  node((2, 2), "LEADER_DEADLOCK", kind: "rect", ratio: 6);
  node((-2, 2), "LEADER_GOTO_DEADLOCK", kind: "rect", ratio: 7.5);
  node((-2.5, 1.3), "LEADER_RESTORE", kind: "rect", ratio: 5.5);
  node((2.5, 1.3), "LEADER_WAITFOR", kind: "rect", ratio: 6);
  node((3, 0.5), "LEADER_COMM_ARRIVED", kind: "rect", ratio: 7);
  node((3, -0.4), "LEADER_OPENLOCK", kind: "rect", ratio: 6);
}))
  
==== Différenciation des FSM selon les types d'agents
Notre système comporte trois types d'agents avec des automates distincts adaptés à leurs fonctions spécifiques:



FSM des agents collecteurs (FsmCollectAgent)
L'automate des agents collecteurs est le plus complexe avec 59 états distincts organisés en groupes fonctionnels. Cette complexité reflète la diversité des tâches qu'ils doivent accomplir:
- L'automate inclut tous les groupes d'états (EXPLORE, COLLECT, DROP, CHEST, LEADER, FOLLOWER, END)
- Les transitions entre états sont conditionnées par des codes de retour numériques indiquant le résultat des actions tentées (succès/échec/nécessité de coordination)

#figure(caption: [Représentation de l'automate des comportements de collecte -- Partie 2], canvas(length: 1.8cm, {
  node((-0, -2), "COLLECT", kind: "rect", ratio: 3);
  node((-4, -2), "CHEST_NEGOTIATION", kind: "rect", ratio: 6);

  node((-4, -3), "FOLLOWER", kind: "rect", ratio: 4);
  node((-4, -4), "FOLLOWER_COMM", kind: "rect", ratio: 6);
  node((-4, -5), "FOLLOWER_COMM_WAYPOINT", kind: "rect", ratio: 8.5);
  node((-1, -4), "FOLLOWER_GOTO", kind: "rect", ratio: 5);
  node((-1, -3), "FOLLOWER_DEADLOCK", kind: "rect", ratio: 7);
  node((3, -3), "FOLLOWER_GOTO_DEADLOCK", kind: "rect", ratio: 8);
  node((2, -5), "FOLLOWER_RESTORE", kind: "rect", ratio: 6.5);
  node((0, -1), "FOLLOWER_OPENLOCK", kind: "rect", ratio: 6.5);
  node((2, 0), "END_LOCATE", kind: "rect", ratio: 5);
  node((-0.7, 0), "END_GOTO", kind: "rect", ratio: 4);
  node((0, 1), "END_WAIT", kind: "rect", ratio: 3);
  node((-3, 1), "END_COMM", kind: "rect", ratio: 3);
  node((-4, 2), "END_COMM_SHARE", kind: "rect", ratio: 6.5);
  node((0, 2), "END_COMM_MEETING", kind: "rect", ratio: 7);
  node((-3, 0), "END_DEADLOCK", kind: "rect", ratio: 6);
  node((-4, -1), "END_GOTO_DEADLOCK", kind: "rect", ratio: 7);

  arc("COLLECT", "END_LOCATE", alpha: 0.2, value: [1]);
  arc("CHEST_NEGOTIATION", "COLLECT", alpha: 0.2);
  arc("CHEST_NEGOTIATION", "FOLLOWER", alpha: 0.2, value: [2]);
  arc("FOLLOWER", "FOLLOWER_COMM", alpha: 0.2);
  arc("FOLLOWER_COMM", "FOLLOWER", alpha: 0.2);
  arc("FOLLOWER_COMM", "FOLLOWER_COMM_WAYPOINT", alpha: 0.2, value: [1]);
  arc("FOLLOWER_COMM", "FOLLOWER_OPENLOCK", alpha: -0.2, value: [2]);
  arc("FOLLOWER_COMM_WAYPOINT", "FOLLOWER_GOTO", alpha: 0.2);
  arc("FOLLOWER_GOTO", "FOLLOWER_GOTO", alpha: 0.2);
  arc("FOLLOWER_GOTO", "FOLLOWER_COMM", alpha: 0.2, value: [1]);
  arc("FOLLOWER_GOTO", "FOLLOWER_DEADLOCK", alpha: 0.2, value: [2]);
  arc("FOLLOWER_DEADLOCK", "FOLLOWER_GOTO_DEADLOCK", alpha: 0.2);
  arc("FOLLOWER_GOTO_DEADLOCK", "FOLLOWER_GOTO_DEADLOCK", alpha: 0.2);
  arc("FOLLOWER_GOTO_DEADLOCK", "FOLLOWER_RESTORE", alpha: 0.2, value: [1]);
  arc("FOLLOWER_GOTO_DEADLOCK", "FOLLOWER_DEADLOCK", alpha: 0.2, value: [2]);
  arc("FOLLOWER_RESTORE", "FOLLOWER_GOTO", alpha: 0.2);
  arc("FOLLOWER_OPENLOCK", "COLLECT", alpha: 0.2);
  arc("END_LOCATE", "END_GOTO", alpha: 0.2);
  arc("END_GOTO", "END_GOTO", alpha: 0.2);
  arc("END_GOTO", "END_WAIT", alpha: 0.2, value: [1]);
  arc("END_GOTO", "END_DEADLOCK", alpha: 0.2, value: [2]);
  arc("END_WAIT", "END_COMM", alpha: 0.2);
  arc("END_COMM", "END_WAIT", alpha: 0.2);
  arc("END_COMM", "END_COMM_SHARE", alpha: 0.2, value: [1]);
  arc("END_COMM", "END_DEADLOCK", alpha: 0.2, value: [2]);
  arc("END_COMM_SHARE", "END_COMM_MEETING", alpha: 0.2);
  arc("END_COMM_MEETING", "END_WAIT", alpha: 0.2);
  arc("END_DEADLOCK", "END_GOTO_DEADLOCK", alpha: 0.2);
  arc("END_GOTO_DEADLOCK", "END_GOTO_DEADLOCK", alpha: 0.2);
  arc("END_GOTO_DEADLOCK", "END_LOCATE", alpha: 0.2, value: [1]);
  arc("END_GOTO_DEADLOCK", "END_DEADLOCK", alpha: 0.2, value: [2]);

  node((-0, -2), "COLLECT", kind: "rect", ratio: 3);
  node((-4, -2), "CHEST_NEGOTIATION", kind: "rect", ratio: 6);

  node((-4, -3), "FOLLOWER", kind: "rect", ratio: 4);
  node((-4, -4), "FOLLOWER_COMM", kind: "rect", ratio: 6);
  node((-4, -5), "FOLLOWER_COMM_WAYPOINT", kind: "rect", ratio: 8.5);
  node((-1, -4), "FOLLOWER_GOTO", kind: "rect", ratio: 5);
  node((-1, -3), "FOLLOWER_DEADLOCK", kind: "rect", ratio: 7);
  node((3, -3), "FOLLOWER_GOTO_DEADLOCK", kind: "rect", ratio: 8);
  node((2, -5), "FOLLOWER_RESTORE", kind: "rect", ratio: 6.5);
  node((0, -1), "FOLLOWER_OPENLOCK", kind: "rect", ratio: 6.5);
  node((2, 0), "END_LOCATE", kind: "rect", ratio: 5);
  node((-0.7, 0), "END_GOTO", kind: "rect", ratio: 4);
  node((0, 1), "END_WAIT", kind: "rect", ratio: 3);
  node((-3, 1), "END_COMM", kind: "rect", ratio: 3);
  node((-4, 2), "END_COMM_SHARE", kind: "rect", ratio: 6.5);
  node((0, 2), "END_COMM_MEETING", kind: "rect", ratio: 7);
  node((-3, 0), "END_DEADLOCK", kind: "rect", ratio: 6);
  node((-4, -1), "END_GOTO_DEADLOCK", kind: "rect", ratio: 7);
}))



FSM des agents explorateurs (FsmExploreAgent)
L'automate des agents explorateurs est une version simplifiée comportant 26 états qui se concentrent sur la cartographie de l'environnement:
- L'automate inclut principalement les groupes EXPLORE, FOLLOWER et END
- Il ne possède pas les états de collecte active (COLLECT) ou de dépôt (DROP)
- Les états FOLLOWER permettent à l'agent d'assumer un rôle de support pour l'ouverture des coffres sans capacité de collecte

FSM des agents silos (FsmSiloAgent)
L'automate des silos est le plus minimaliste avec 18 états, reflétant leur rôle statique de point de dépôt:
- L'automate alterne entre deux modes principaux: EXPLORE pour la phase initiale et COLLECT pour le positionnement final
- Phase COLLECT unique incluant les états COLLECT_WAIT qui permet au silo de rester stationnaire tout en maintenant des communications



==== Logique de transition
Les transitions d'états au sein du système sont contrôlées par divers facteurs déterminant son comportement. L'environnement influence directement ces transitions : par exemple, la découverte d'un trésor provoque le passage de l'état GOTO à OPENLOCK, tandis que la rencontre d'un obstacle entraîne un état DEADLOCK. L'état interne du système joue également un rôle, comme lorsque l'on rempli le sac à dos et que l'agent passe de COLLECT à DROP_LOCATE_SILO. La communication entre agents constitue un autre facteur important, notamment lors de la réception d'une demande de coordination qui déclenche une négociation. Enfin, le système intègre des mécanismes de récupération après erreur, utilisant des compteurs de blocage et des états de restauration pour revenir à un fonctionnement normal. Ces transitions, illustrées par les exemples précédents, permettent d'adapter le comportement du système aux différentes situations rencontrées.

== Choix
// Présentation des choix associés à : exploration, communication, coordination et collecte

Dans cette partie, nous présentons les choix qui ont guidé notre implémentation, leurs avantages et inconvénients dans un contexte de communication limitée et d'environnement partiellement observable. Un choix fondamental a orienté notre approche : nous privilégions des solutions simples et "mono-agent", où chaque agent suit des comportements prévisibles mais introduit une part d'aléatoire pour éviter le déterminisme qui pourrait conduire plusieurs agents à des conclusions identiques et créer des interblocages. Cette préférence repose sur l'observation qu'une solution nécessitant une communication constante pour la coordination d'actions s'avère généralement moins robuste qu'une approche plus "locale", particulièrement dans un environnement aux communications limitées et peu fiables.


=== Stratégie d'exploration
Notre exploration repose sur l'identification et la sélection de frontières entre zones connues et inconnues. La méthode `findClosestOpenNode()` évalue les nœuds candidats selon deux critères principaux : la distance depuis la position actuelle et l'état d'exploration du nœud. Lorsque ces critères entrent en conflit, notre algorithme de min-max regret (classe `Computes`) permet une décision équilibrée entre exploitation locale et exploration globale.

Pour coordonner l'exploration entre agents, la classe `PlanExplorationBehaviour` implémente un mécanisme de planification collaborative. Lors des rencontres, les agents partagent leurs cartes, négocient des points de rencontre futurs et se répartissent des zones distinctes d'exploration. Cette répartition utilise un échantillonnage pondéré qui introduit une variabilité contrôlée tout en favorisant l'efficacité.

=== Protocole de communication
Notre protocole, implémenté dans la classe `Protocols`, utilise un mécanisme de négociation en trois phases inspiré de protocoles de handshakes :
1. Phase d'invitation : diffusion d'un message aux agents à portée
2. Phase de réponse : les récepteurs répondent avec leurs priorités
3. Phase de confirmation : sélection d'un interlocuteur et d'un protocole

Ce système permet de gérer efficacement les priorités de communication via la méthode `shouldCommunicateWith()`. Pour optimiser la bande passante, la classe `ShareBrainBehaviour` implémente un partage incrémental d'informations, transmettant uniquement les nœuds inconnus du destinataire grâce au suivi des connaissances partagées (`agentKnownNodes`).

Notre protocole conteste directement plusieurs illusions des systèmes distribués : nous reconnaissons que la bande passante n'est pas infinie, que le réseau n'est pas toujours fiable, et que les communications ont un coût. Il présente des inconvénients : le rayon de communication crée des "îlots d'information" et l'absence de mécanisme d'écho limite la propagation des connaissances. Finalement, le protocole est conçu pour le dialogue entre deux agents, ce qui limite la coordination pour des contextes de coalitions de $n$-agents.

#figure(caption: [Hypothèse agents et réseaux], table(columns: 2,
[Les agents s’exécutent chacun leur tour], [Faux, les agents sont asynchrones sur Jade],
[Les agents sont homogènes et s’exécutent à la même vitesse], [Semi-vrai, des vitesses différentes ralentissent certaines actions coordonnées],
[Les agents ont accès à des ressources illimitées], [Vrai, suffisement de mémoire pour les graphes testés],
[Les agents sont fiables], [Vrai, les agents appartiennet tous à la même équipe],
[Les agent sont sûrs], [Vrai, les agents ne peuvent pas quitter le système],
[Le nombre d’agent ne varie pas au cour du temps], [Vrai, aucun agent ne rentre ou ne sort après le lancement],
[Les agents disposent d’une vision globale], [Faux, les agents ont une communication limitée],
[Les communications respectent les 8 illusions des systèmes distribués], [Vrai],
[], [],
[Le réseau est fiable], [Vrai, sur la même machine],
[La latence est nulle], [Vrai, sur la même machine],
[La bande passante est infinie], [Vrai, la mémoire est bien supérieure aux communications],
[Le réseau est sécurité], [Vrai, sur la même machine],
[La topologie est fixe], [Vrai, Jade initialise la topologie au départ],
[Le réseau dispose d'UN administrateur], [Vrai, nous lançons la plateforme],
[Les coûts de communications sont nuls], [Vrai, sur la même machine],
[Le réseau est homogène], [Vrai, car une unique machine],
))

=== Mécanismes de coordination
Notre coordination pour ouvrir des coffres repose sur un modèle leader-suiveur structuré dans les états `LEADER_*` et `FOLLOWER_*` de notre FSM. Lorsqu'un agent découvre un coffre nécessitant des compétences supplémentaires, `CoordinationInitBehaviour` identifie un partenaire potentiel, puis `TreasureCoordinationNegotiationBehaviour` détermine les rôles et la cible prioritaire.

La coordination pratique s'effectue via un système de guidage par points de passage (classes `LeaderGuidanceBehaviour` et `WaypointCommunicationBehaviour`) qui permet une navigation coordonnée même dans un environnement complexe.

Cette approche leader-suiveur présente l'avantage de simplifier la coordination : un agent (le leader) peut se concentrer sur l'objectif tout en s'assurant de ne jamais être trop éloigné du suiveur, tandis que le second agent suit progressivement les indications reçues. Cette structure évite les situations de compétition et d'interblocage qui pourraient survenir si les deux agents tentaient d'atteindre l'objectif selon leurs propres stratégies.

=== Stratégie de collecte
Notre sélection des ressources s'appuie sur la méthode `findOptimalTreasureNode()` qui évalue les trésors selon leur valeur, la distance et l'adéquation des compétences. La gestion du sac à dos est assurée par la classe `AgentData` qui déclenche le retour au silo lorsque nécessaire, tandis que `findOptimalWaitingNode()` sélectionne le point de dépôt optimal.

Ces choix représentent notre tentative d'équilibrer la complexité algorithmique et l'efficacité pratique, tout en reconnaissant les limites inhérentes aux systèmes multi-agents dans un environnement dynamique et partiellement observable.


== Algorithmes
// Pour les différents algorithmes présentés : indiquer leurs forces et limites, leur complexité (temps, mémoire, communication) et discuter de leur optimalité et critère d'arrêt.
Cette section présente les trois algorithmes fondamentaux de notre système, leurs principes théoriques et leurs limites.

=== Protocole de communication handshake à trois phases

Notre protocole de communication résout le problème de l'établissement d'un canal fiable dans un environnement asynchrone multi-agents.

Le mécanisme s'articule en trois phases:

1. Phase d'invitation: Un agent diffuse un message "handshake0" à tous les agents à portée.
2. Phase de réponse: Les agents répondent par "handshake1" avec leurs priorités.
3. Phase de confirmation: L'émetteur sélectionne un partenaire et confirme par "handshake2".

La sélection s'effectue selon la priorité des messages (haute pour les coordinations, moyenne pour les échanges de carte) et, en cas d'égalité, selon l'ordre lexicographique des identifiants.

Ce protocole garantit la terminaison grâce à un timeout explicite et présente une complexité de O(n) en temps où n est le nombre d'agents à portée. Sa principale limitation vient du broadcast initial non ciblé qui peut interrompre des communications en cours si un agent avec une priorité plus élevée entre dans le rayon de communication.

=== MinMax Regret pour la prise de décision multicritère

Notre algorithme MinMax Regret résout les problèmes d'optimisation multicritère quand l'importance relative des critères est incertaine.

L'approche calcule d'abord, pour chaque option et critère, le "regret" (écart entre la valeur de l'option et la valeur optimale). Ces regrets sont normalisés par rapport au maximum de chaque critère, puis pondérés selon l'importance des critères. L'algorithme sélectionne l'option qui minimise le regret maximum.

Pour introduire une variabilité contrôlée, nous combinons cette méthode avec un échantillonnage softmax, transformant les scores en distribution de probabilité. Cette approche, utilisée dans la répartition des zones d'exploration, évite que tous les agents ne convergent vers les mêmes régions.

La principale limitation est l'hypothèse d'indépendance des critères et la sensibilité aux valeurs extrêmes qui peuvent biaiser la normalisation.

=== Mécanisme adaptatif de résolution des blocages

Notre système de résolution des blocages implémente une stratégie d'évitement adaptatif ajustant progressivement le rayon de recherche.

L'algorithme construit un sous-graphe excluant les nœuds occupés, définit un rayon de recherche proportionnel au compteur de blocage et finalement sélectionne aléatoirement une destination dans ce rayon.

Le compteur s'incrémente à chaque échec de mouvement et se réinitialise après un déplacement réussi, intensifiant progressivement les efforts pour résoudre une situation persistante.

Ce mécanisme s'applique également aux demandes de libération d'espace, à la gestion des silos en veille, et aux situations de coordination interrompue.

Sa limitation principale est l'absence de garantie d'optimalité: dans certaines topologies complexes, l'agent peut osciller entre positions sous-optimales sans résoudre efficacement le blocage.

== Conclusion
// Conclusion : Synthèse, Regard critique sur votre travail, extensions et améliorations possibles

Ce projet nous a permis de développer un système multi-agents pour la résolution coopérative d'une tâche de chasse au trésor dans un environnement inconnu et dynamique. Notre approche, centrée sur une architecture à machine à états finis, offre un cadre structuré pour gérer la complexité du problème tout en permettant l'émergence de comportements collectifs efficaces.

L'utilisation d'un automate à états finis s'est révélée particulièrement adaptée au contexte du projet. Cette structure nous a permis d'organiser clairement les responsabilités des agents et de définir les transitions entre comportements en fonction des conditions environnementales et des objectifs internes. La décomposition hiérarchique des états a facilité l'implémentation de stratégies d'exploration, de collecte et de coordination, tout en maintenant une vision claire du comportement global du système.

Notre système a surmonté plusieurs défis fondamentaux des systèmes multi-agents. Notre protocole de communication à trois phases assure un établissement fiable des canaux de communication malgré l'asynchronisme inhérent au système. L'algorithme de min-max regret, couplé à un échantillonnage softmax, permet des décisions équilibrées face à des critères multiples tout en introduisant une variabilité bénéfique. Le mécanisme adaptatif de résolution des blocages offre une solution robuste aux situations d'impasse fréquentes dans les environnements partagés.

Des améliorations pourraient néanmoins enrichir notre système. Une extension du mécanisme de coordination permettrait de supporter des collaborations impliquant plus de deux agents, élargissant ainsi le spectre des tâches réalisables. L'intégration d'un modèle probabiliste pour le suivi des ressources renforcerait la robustesse face aux actions des adversaires. Une optimisation plus poussée des communications réduirait la charge du réseau tout en maximisant la pertinence des échanges.

Notre projet illustre l'équilibre nécessaire entre complexité algorithmique et efficacité pratique dans les systèmes multi-agents. Nous avons privilégié des approches pragmatiques qui fonctionnent dans le cadre des contraintes imposées, tout en reconnaissant les compromis effectués. Cette démarche reflète une réalité du développement des systèmes multi-agents : la robustesse face à l'incertitude et aux contraintes de communication constitue souvent un objectif plus réaliste que l'optimalité théorique.

En définitive, notre système démontre qu'une architecture à états finis, combinée à des algorithmes adaptés aux contraintes des environnements multi-agents, peut produire des comportements collectifs efficaces malgré l'absence de contrôle centralisé et les limitations de communication. Cette conclusion ouvre la voie à des applications élargies dans des contextes où la robustesse et l'adaptabilité représentent des qualités essentielles.

