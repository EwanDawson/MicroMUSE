package net.lazygun.micromuse

import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Path
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.traversal.Evaluation
import org.neo4j.graphdb.traversal.Evaluator
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.Traverser
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.test.TestGraphDatabaseFactory
import spock.lang.Specification

import static net.lazygun.micromuse.Neo4JMuseMap.ROOM
import static net.lazygun.micromuse.Neo4JMuseMap.RelTypes.EXIT
import static net.lazygun.micromuse.Neo4JMuseMap.TELEPORTABLE
import static net.lazygun.micromuse.Neo4JMuseMap.UNEXPLORED
import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * Created by ewan on 26/03/2014.
 */
class Neo4JMuseMapTest extends Specification {

  GraphDatabaseService graphDb
  Neo4JMuseMap map
  ExecutionEngine cypher
  Transaction tx
  Traverser traverser

  List<String> homeExits = ('A'..'C').toList()

  def setup() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
    map = new Neo4JMuseMap(graphDb)
    cypher = new ExecutionEngine(graphDb)
    tx = graphDb.beginTx()

    def homeNode = graphDb.createNode(ROOM, TELEPORTABLE)
    homeNode.setProperty("name", "Home")
    homeNode.setProperty("location", "#0")
    homeNode.setProperty("description", "")
    homeExits.each { exit ->
      def unexploredNode = graphDb.createNode(ROOM, UNEXPLORED)
      unexploredNode.setProperty("name", UnexploredRoom.NAME)
      def rel = homeNode.createRelationshipTo(unexploredNode, EXIT)
      rel.setProperty("name", exit)
      unexploredNode
    }
    assert map.home.id == 0

    traverser = graphDb.traversalDescription()
      .breadthFirst()
      .relationships(EXIT, OUTGOING)
      .uniqueness(Uniqueness.NODE_PATH)
      .evaluator([evaluate: { path ->
        if (path.length() == 0) {
          return path.endNode().getRelationships(OUTGOING).iterator().hasNext() ?
            Evaluation.EXCLUDE_AND_CONTINUE :
            Evaluation.INCLUDE_AND_PRUNE;
        }
        for (Relationship rel : path.endNode().getRelationships(OUTGOING)) {
          if (rel.getEndNode().getId() != path.lastRelationship().getStartNode().getId()) {
            return Evaluation.EXCLUDE_AND_CONTINUE;
          }
        }
        return Evaluation.INCLUDE_AND_PRUNE;
      }] as Evaluator)
      .traverse(homeNode)

    printMap("Starting graph:")
  }

  def cleanup() {
    printMap("Finishing graph:")
    tx.close()
    graphDb.shutdown()
  }

  def "replace unexplored room with regular room"() {
    given:
    def room = new Room("Room A", "Test room A", ('1'..'3').toList())
    def link = new Link(map.home, homeExits[0], room)
    when:
    map.createLink(link)
    then:
    def result = cypher.execute("start n=node(0) match n-[e:EXIT]->(m) where e.name='${homeExits[0]}' return m").toList()
    result.size() == 1
    Node roomNode = result[0].m
    roomNode.getProperty("name") == "Room A"
    roomNode.getRelationships(INCOMING).toList().size() == 1
    roomNode.getRelationships(OUTGOING).toList().size() == 3
  }

  void printMap(title) {
    println(title)
    traverser.iterator().toList().each { p ->
      p.each { e ->
        switch (e) {
          case Node:
            print "(${e.getProperty('name')})"
            break
          case Relationship:
            print "-[${e.getProperty('name')}]->"
        }
      }
      println ""
    }
    println ""
  }
}
