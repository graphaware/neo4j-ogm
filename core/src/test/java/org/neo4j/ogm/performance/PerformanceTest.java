package org.neo4j.ogm.performance;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;


public class PerformanceTest {

    SessionFactory sf = new SessionFactory("gh327");
    Session session;

    @Before
    public void setUp() throws Exception {
        session = sf.openSession();
//        setupData();
    }

    private void setupData() {
        session.purgeDatabase();

        for (int i=0; i<500; i++) {
            Primary primary = new Primary("Primary" + i);
            for (int j=0; j<12; j++) {
                Level1Node level1Node = new Level1Node("level1-" + j);
                primary.level1Nodes.add(level1Node);
                for (int k=0; k<5; k++) {
                    Level2Node level2Node = new Level2Node("level2-" + k);
                    level1Node.level2Nodes.add(level2Node);
                }
            }
            session.save(primary);
        }
    }

    @Test
    public void test() throws Exception {

        Iterable res = session.query(Primary.class, "MATCH (n:Primary) WITH n MATCH path=(n)-[*0..]->() RETURN path", new HashMap());
        Assert.assertTrue(res.iterator().hasNext());
    }
}
