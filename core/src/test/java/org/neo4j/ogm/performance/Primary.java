package org.neo4j.ogm.performance;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Primary {

	public Long id;
	public String name;
	@Relationship(type = "POINTS_TO")
	public List<Level1Node> level1Nodes = new ArrayList<>();

	public Primary() {
	}

	public Primary(String name) {
		this.name = name;
	}
}
