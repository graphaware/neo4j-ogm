package org.neo4j.ogm.performance;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Level1Node {

	public Long id;
	public String name;
	@Relationship(type = "POINTS_TO")
	public List<Level2Node> level2Nodes = new ArrayList<>();

	public Level1Node() {
	}

	public Level1Node(String name) {
		this.name = name;
	}
}
