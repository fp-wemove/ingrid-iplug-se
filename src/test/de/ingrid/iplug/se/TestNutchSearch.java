/*
 * Copyright (c) 1997-2005 by media style GmbH
 * 
 * $Source: /cvs/asp-search/src/java/com/ms/aspsearch/PermissionDeniedException.java,v $
 */

package de.ingrid.iplug.se;

import java.io.File;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;

import de.ingrid.utils.IngridHit;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.query.ClauseQuery;
import de.ingrid.utils.query.FieldQuery;
import de.ingrid.utils.query.IngridQuery;
import de.ingrid.utils.query.TermQuery;
import de.ingrid.utils.queryparser.QueryStringParser;

public class TestNutchSearch extends TestCase {

  private Configuration fConfiguration;
  private File fIndex;

  protected void setUp() throws Exception {
    this.fConfiguration = NutchConfiguration.create();
    //this.fConfiguration.set("plugin.folders",
    //"/Users/joa23/Documents/workspace/nutch-trunk/src/plugin");
    this.fConfiguration.set("plugin.folders",
        "/Users/mb/develop/workspace/ingrid-nutch-plugins/src/plugin");
    this.fConfiguration
        .set(
            "plugin.includes",
            "protocol-http|urlfilter-regex|parse-(text|html|js)|index-(basic|sns)|query-(basic|ingrid-se)");
    //this.fIndex = new File("./testIndex");
    //this.fIndex = new File("/Users/mb/segments");
    this.fIndex = new File("./test-resources");
  }

  protected void tearDown() throws Exception {
    this.fConfiguration.set("plugin.folders", "plugins");
    this.fConfiguration
        .set(
            "plugin.includes",
            "protocol-http|urlfilter-regex|parse-(text|html|js)|index-basic|query-(basic|site|url)");
  }

  /**
   * @throws Exception
   */
  public void testSearch() throws Exception {
    
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId", this.fConfiguration);
    IngridQuery query = QueryStringParser.parse("partner:bund");
    IngridHits hits = searcher.search(query, 0, 100);
    assertTrue(hits.size() > 0);
  }

  

  /**
   * @throws Exception
   */
  public void testOrFieldQuery() throws Exception {
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);

    IngridQuery query = new IngridQuery();
    query.addTerm(new TermQuery(true, false, "wasser"));
    query.put("ranking", "date");
    ClauseQuery cq = new ClauseQuery(true, false);
    cq.addField(new FieldQuery(false, false, "topic", "gentechnik"));
    cq.addField(new FieldQuery(false, false, "topic", "abfall"));
   
    query.addClause(cq);


    IngridHits hits = searcher.search(query, 0, 100);
    assertTrue(hits.length() > 0);
    IngridHit[] hits2 = hits.getHits();
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit);
    }
  }
  
  /**
   * @throws Exception
   */
  public void testOrFieldQueryClause() throws Exception {
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);

    IngridQuery query = new IngridQuery();
    query.addTerm(new TermQuery(true, false, "wasser"));
    query.addField(new FieldQuery(true, false, "datatype", "topics"));
    ClauseQuery cq = new ClauseQuery(true, false);
    cq.addField(new FieldQuery(false, false, "topic", "gentechnik"));
    cq.addField(new FieldQuery(false, false, "topic", "abfall"));
    query.addClause(cq);


    IngridHits hits = searcher.search(query, 0, 100);
    assertTrue(hits.length() > 0);
    IngridHit[] hits2 = hits.getHits();
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit);
    }
  }

  
  public void testOrFieldQuery2() throws Exception {

    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);

    IngridQuery query = new IngridQuery();
    query.addTerm(new TermQuery(true, false, "wasser"));
    query.put("ranking", "date");
    ClauseQuery cq = new ClauseQuery(true, false);
    cq.addField(new FieldQuery(false, false, "topic", "gentechnik"));
    cq.addField(new FieldQuery(false, false, "topic", "abfall"));
   
    
    ClauseQuery cq2 = new ClauseQuery(false, false);
    cq2.addField(new FieldQuery(true, false, "topic", "gentechnik2"));
    cq2.addField(new FieldQuery(false, false, "topic", "abfall2"));
   
    cq.addClause(cq2);
    query.addClause(cq);


    IngridHits hits = searcher.search(query, 0, 100);
    assertTrue(hits.length() > 0);
    IngridHit[] hits2 = hits.getHits();
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit);
    }
  }
  
  public void testFunctCategory() throws Exception {
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);

    IngridQuery query = new IngridQuery();
    query.addField(new FieldQuery(true, false, "datatype",
        "topics"));
    ClauseQuery cq2 = new ClauseQuery(true, false);
    cq2.addField(new FieldQuery(true, false, "funct_category", "rechtliches"));
    query.addClause(cq2);
    IngridHits hits = searcher.search(query, 0, 100);
    assertTrue(hits.length()>0);
    
  }
  
  
  public void testGroupByPartner() throws Exception {
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridQuery query = new IngridQuery();
    query.put("grouped", IngridQuery.GROUPED_BY_PARTNER);
    System.out.println(query.getGrouped());
    query.addField(new FieldQuery(true, false, "datatype",
        "topics"));
    
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    boolean loopOne = false;
    boolean loopTwo = false;
    for (int i = 0; i < hits2.length; i++) {
      loopOne = true;
      IngridHit hit = hits2[i];
      String[] groupedFileds = hit.getGroupedFileds();
      for (int j = 0; j < groupedFileds.length; j++) {
        loopTwo = true;
        String string = groupedFileds[j];
        assertNotNull(string);
      }
    }
    assertTrue(loopOne);
    assertTrue(loopTwo);
  }
  
  
  public void testTime() throws Exception {
    IngridQuery query = new IngridQuery();
    query.addField(new FieldQuery(true, false, "t0",
        "2006-01-01"));
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
  }
  
  
  public void testRankingDate() throws Exception {
    
    IngridQuery query = new IngridQuery();
    query.addField(new FieldQuery(true, false, "datatype", "topics"));
    query.put("ranking", "date");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit.toString());
      System.out.println(hit.getScore());
    }
  }
  
  public void testOrClause() throws Exception {
    IngridQuery query = QueryStringParser.parse("(wasser OR erde)");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertEquals(100, hits2.length);
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit.toString());
    }
  }
  
  public void testOr() throws Exception {
    IngridQuery query = QueryStringParser.parse("wasser OR erde");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertEquals(100, hits2.length);
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit.toString());
    }
  }
  
  public void testPhrase() throws Exception {
    IngridQuery query = QueryStringParser.parse("wasser erde");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertEquals(100, hits2.length);
    for (int i = 0; i < hits2.length; i++) {
      IngridHit hit = hits2[i];
      System.out.println(hit.toString());
    }
  }
  
  public void testGeoIntersect() throws Exception {
    IngridQuery query = QueryStringParser.parse("HVZ coord:intersect x1:1 x2:20 y1:10 y2:60");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertTrue(hits.length() == 0);
    
    query = QueryStringParser.parse("HVZ coord:intersect x1:9 x2:12 y1:50.1 y2:51");
    searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    hits = searcher.search(query, 0, 100);
    hits2 = hits.getHits();
    assertTrue(hits.length() > 0);

    query = QueryStringParser.parse("HVZ coord:intersect x1:11.2 x2:11.8 y1:50.1 y2:50.6");
    searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    hits = searcher.search(query, 0, 100);
    hits2 = hits.getHits();
    assertTrue(hits.length() == 0);
    
  }
  
  public void testGeoInside() throws Exception {
    IngridQuery query = QueryStringParser.parse("HVZ coord:inside x1:9 x2:16 y1:45 y2:60");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertTrue(hits.length() > 0);
  }

  public void testGeoInclude() throws Exception {
    IngridQuery query = QueryStringParser.parse("HVZ coord:include x1:10.5 x2:10.7 y1:50.5 y2:51.3");
    NutchSearcher searcher = new NutchSearcher(this.fIndex, "testId",
        this.fConfiguration);
    IngridHits hits = searcher.search(query, 0, 100);
    IngridHit[] hits2 = hits.getHits();
    assertTrue(hits.length() > 0);
  }
  
}
