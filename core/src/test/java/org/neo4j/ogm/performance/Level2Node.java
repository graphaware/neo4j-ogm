package org.neo4j.ogm.performance;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Level2Node {

	public Long id;
	public String name;

	public Level2Node() {
	}

	public Level2Node(String name) {
		this.name = name;
	}
}
