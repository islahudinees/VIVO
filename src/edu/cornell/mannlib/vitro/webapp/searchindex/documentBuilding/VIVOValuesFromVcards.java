/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.searchindex.documentBuilding;

import static edu.cornell.mannlib.vitro.webapp.search.VitroSearchTermNames.ALLTEXT;
import static edu.cornell.mannlib.vitro.webapp.search.VitroSearchTermNames.ALLTEXTUNSTEMMED;
import static edu.cornell.mannlib.vitro.webapp.search.VitroSearchTermNames.PREFERRED_TITLE;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ContextModelAccess;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchInputDocument;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.impl.RDFServiceUtils;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.ContextModelsUser;

/**
 * If there are any VCards on this Individual with Title objects, store the text
 * in the Preferred Title search field, and the ALL_TEXT field.
 * 
 * If there are any VCards on this Individual with EMail objects, store the text
 * in the ALL_TEXT field.
 */
public class VIVOValuesFromVcards implements DocumentModifier, ContextModelsUser {
	private static final Log log = LogFactory
			.getLog(VIVOValuesFromVcards.class);

	private static final String PREFERRED_TITLE_QUERY = ""
			+ "prefix vcard: <http://www.w3.org/2006/vcard/ns#> \n"
			+ "prefix obo: <http://purl.obolibrary.org/obo/> \n\n"
			+ "SELECT ?title WHERE { \n" //
			+ "  ?uri obo:ARG_2000028 ?card . \n"
			+ "  ?card a vcard:Individual . \n"
			+ "  ?card vcard:hasTitle ?titleHolder . \n"
			+ "  ?titleHolder vcard:title ?title . \n" //
			+ "}";

	private static final ResultParser PREFERRED_TITLE_PARSER = new ResultParser() {
		@Override
		public void parse(String uri, QuerySolution solution, SearchInputDocument doc) {
			String title = getLiteralValue(solution, "title");
			if (StringUtils.isNotBlank(title)) {
				doc.addField(PREFERRED_TITLE, title);
				doc.addField(ALLTEXT, title);
				doc.addField(ALLTEXTUNSTEMMED, title);
				log.debug("Preferred Title for " + uri + ": '" + title + "'");
			}
		}
	};

	private static final String EMAIL_QUERY = ""
			+ "prefix vcard: <http://www.w3.org/2006/vcard/ns#> \n"
			+ "prefix obo: <http://purl.obolibrary.org/obo/> \n\n"
			+ "SELECT ?email WHERE { \n" //
			+ "  ?uri obo:ARG_2000028 ?card . \n"
			+ "  ?card a vcard:Individual . \n"
			+ "  ?card vcard:hasEmail ?emailHolder . \n"
			+ "  ?emailHolder vcard:email ?email . \n" //
			+ "}";

	private static final ResultParser EMAIL_PARSER = new ResultParser() {
		@Override
		public void parse(String uri, QuerySolution solution,
				SearchInputDocument doc) {
			String email = getLiteralValue(solution, "email");
			if (StringUtils.isNotBlank(email)) {
				doc.addField(ALLTEXT, email);
				doc.addField(ALLTEXTUNSTEMMED, email);
				log.debug("Email for " + uri + ": '" + email + "'");
			}
		}};

	private RDFService rdfService;
	private boolean shutdown = false;

	@Override
	public void setContextModels(ContextModelAccess models) {
		this.rdfService = models.getRDFService();
	}

	@Override
	public void modifyDocument(Individual individual, SearchInputDocument doc) {
		if (individual == null)
			return;

		processQuery(individual, PREFERRED_TITLE_QUERY, PREFERRED_TITLE_PARSER,
				doc);
		processQuery(individual, EMAIL_QUERY, EMAIL_PARSER, doc);
	}

	private void processQuery(Individual individual, String queryTemplate,
			ResultParser resultParser, SearchInputDocument doc) {
		String uri = "<" + individual.getURI() + "> ";
		String query = queryTemplate.replaceAll("\\?uri", uri);

		try {
			ResultSet results = RDFServiceUtils.sparqlSelectQuery(query,
					rdfService);
			if (results != null) {
				while (results.hasNext()) {
					log.debug("Next solution");
					QuerySolution solution = results.nextSolution();
					resultParser.parse(uri, solution, doc);
				}
			}
		} catch (Exception e) {
			if (!shutdown) {
				log.error("problem while running query '" + query + "'", e);
			}
		}
	}

	@Override
	public void shutdown() {
		shutdown = true;
	}


	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[]";
	}

	private abstract static class ResultParser {
		public abstract void parse(String uri, QuerySolution solution, SearchInputDocument doc);

		String getLiteralValue(QuerySolution solution, String name) {
			RDFNode node = solution.get(name);
			if ((node != null) && (node.isLiteral())) {
				String value = node.asLiteral().getString();
				if (StringUtils.isNotBlank(value)) {
					return value;
				}
			}
			return "";
		}
	}
}
