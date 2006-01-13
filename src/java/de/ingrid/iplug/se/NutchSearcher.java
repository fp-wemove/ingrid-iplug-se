package de.ingrid.iplug.se;

import java.io.File;
import java.io.IOException;

import org.apache.nutch.io.FloatWritable;
import org.apache.nutch.searcher.Hit;
import org.apache.nutch.searcher.HitDetails;
import org.apache.nutch.searcher.Hits;
import org.apache.nutch.searcher.NutchBean;
import org.apache.nutch.searcher.Query;

import de.ingrid.iplug.IPlug;
import de.ingrid.utils.IDetailer;
import de.ingrid.utils.IngridDocument;
import de.ingrid.utils.IngridHit;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.query.ClauseQuery;
import de.ingrid.utils.query.FieldQuery;
import de.ingrid.utils.query.IngridQuery;
import de.ingrid.utils.query.TermQuery;
import de.ingrid.utils.queryparser.ParseException;
import de.ingrid.utils.queryparser.QueryStringParser;

/**
 * A nutch Iplug
 * 
 */
public class NutchSearcher implements IPlug, IDetailer {

	private NutchBean fNutchBean;

	private String fProviderId;

	public NutchSearcher(File indexFolder, String providerId)
			throws IOException {
		fNutchBean = new NutchBean(indexFolder);
		fProviderId = providerId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ingrid.utils.ISearcher#search(de.ingrid.utils.query.IngridQuery,
	 *      int, int)
	 */
	public IngridHits search(IngridQuery query, int start, int lenght)
			throws Exception {
		if(start<1){
			start = 1;
		}
		Query nutchQuery = new Query();
		buildNutchQuery(query, nutchQuery);
		Hits hits = fNutchBean.search(nutchQuery, lenght);
		return translateHits(hits, start, lenght);
	}

	/**
	 * @param hits
	 * @param start
	 * @param length
	 * @return ingridHits translated from nutch hits
	 */
	private IngridHits translateHits(Hits hits, int start, int length) {
		int count = length - start;
		count = Math.min(count, hits.getLength());
		IngridHit[] ingridHits = new IngridHit[count];
		for (int i = 0; i < count; i++) {
			Hit hit = hits.getHit(i);
			float score = ((FloatWritable) hit.getSortValue()).get();
			ingridHits[i] = new IngridHit(this.fProviderId,
					hit.getIndexDocNo(), hit.getIndexNo(), score);

		}

		return new IngridHits(fProviderId, hits.getTotal(), ingridHits);
	}

	/**
	 * builds a nutch query
	 * 
	 * @param query
	 * @param out
	 */
	private void buildNutchQuery(IngridQuery query, Query out) {
		// term queries
		TermQuery[] terms = query.getTerms();
		for (int i = 0; i < terms.length; i++) {
			TermQuery termQuery = terms[i];
			boolean prohibited = termQuery.getOperation() == IngridQuery.NOT;
			boolean required = termQuery.getOperation() == IngridQuery.AND;
			boolean optonal = termQuery.getOperation() == IngridQuery.OR;
			if (required) {
				out.addRequiredTerm(termQuery.getTerm());
			} else if (prohibited) {
				out.addProhibitedTerm(termQuery.getTerm());
			} else if (optonal) {
				throw new UnsupportedOperationException(
						"'non required' actually not implemented, INGRID-455");
			}

		}
		// field queries
		FieldQuery[] fields = query.getFields();
		for (int i = 0; i < fields.length; i++) {
			FieldQuery fieldQuery = fields[i];
			boolean prohibited = fieldQuery.getOperation() == IngridQuery.NOT;
			boolean required = fieldQuery.getOperation() == IngridQuery.AND;
			boolean optonal = fieldQuery.getOperation() == IngridQuery.OR;
			if (required) {
				out.addRequiredTerm(fieldQuery.getFieldValue(), fieldQuery
						.getFieldName());
			} else if (prohibited) {
				out.addProhibitedTerm(fieldQuery.getFieldValue(), fieldQuery
						.getFieldName());
			} else if (optonal) {
				throw new UnsupportedOperationException(
						"'non required' actually not implemented, INGRID-455");
			}

		}

		// subclauses

		ClauseQuery[] clauses = query.getClauses();
		for (int i = 0; i < clauses.length; i++) {
			throw new UnsupportedOperationException(
					"'sub Clauses' actually not implemented, INGRID-455");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ingrid.utils.IDetailer#getDetails(de.ingrid.utils.IngridHit)
	 */
	public IngridDocument getDetails(IngridHit ingridHit) throws Exception {
		Hit hit = new Hit(ingridHit.getDataSourceId(), ingridHit
				.getDocumentId());
		HitDetails details = fNutchBean.getDetails(hit);
		IngridDocument document = new IngridDocument();
		int fieldCount = details.getLength();
		for (int i = 0; i < fieldCount; i++) {
			String field = details.getField(i);
			document.put(field, details.getValue(field));
		}
		return document;
	}

	public static void main(String[] args) throws ParseException, Exception {
		String usage = "-d FolderToNutchIndex -q query";
		if (args.length < 4 || !args[0].startsWith("-d")
				|| !args[2].startsWith("-q")) {
			System.err.println(usage);
			System.exit(-1);
		}
		File indexFolder = new File(args[1]);
		String query = args[3];
		NutchSearcher searcher = new NutchSearcher(indexFolder, "aTestId");
		IngridHits hits = searcher
				.search(QueryStringParser.parse(query), 0, 10);
		System.out.println("Results: " + hits.length());
		System.out.println();
		IngridHit[] ingridHits = hits.getHits();
		for (int i = 0; i < ingridHits.length; i++) {
			IngridHit hit = ingridHits[i];
			System.out.println("hist: " + hit.toString());
			System.out.println("details:");
			System.out.println(searcher.getDetails(hit).toString());
		}

	}
}
