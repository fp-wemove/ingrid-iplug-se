/*
 * **************************************************-
 * ingrid-iplug-se-iplug
 * ==================================================
 * Copyright (C) 2014 - 2016 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.iplug.se.webapp.controller.instance;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import de.ingrid.admin.controller.AbstractController;
import de.ingrid.iplug.HeartBeatPlug;
import de.ingrid.iplug.se.webapp.container.Instance;
import de.ingrid.iplug.se.webapp.controller.AdminViews;
import de.ingrid.utils.IRecordLoader;
import de.ingrid.utils.IngridHit;
import de.ingrid.utils.IngridHitDetail;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.query.IngridQuery;
import de.ingrid.utils.queryparser.QueryStringParser;
import de.ingrid.utils.queryparser.TokenMgrError;

/**
 * Control the database parameter page.
 * 
 * @author joachim@wemove.com
 * 
 */
@Controller
@SessionAttributes("plugDescription")
public class SearchController extends AbstractController {

    private final HeartBeatPlug _plug;

    @Autowired
    public SearchController(final HeartBeatPlug plug) throws Exception {
        _plug = plug;
    }

    @RequestMapping(value = { "/iplug-pages/instanceSearch.html" }, method = RequestMethod.GET)
    public String showSearch(final ModelMap modelMap, @RequestParam(value = "instance", required = false) String name) {

        Instance instance = null;
        // if no instance name was found or no belonging directory then show the instance list page
        if (name == null || (instance = InstanceController.getInstanceData( name )) == null) {
            return redirect( AdminViews.SE_LIST_INSTANCES + ".html" );
        } else {
            modelMap.put( "instance", instance );
            return AdminViews.SE_INSTANCE_SEARCH;
        }
    }

    @RequestMapping(value = { "/iplug-pages/instanceSearch.html" }, method = RequestMethod.POST)
    public String doQuery(final ModelMap modelMap,
            @RequestParam(value = "query", required = false) final String queryString,
            @RequestParam("instance") String instance) throws Exception {

        modelMap.addAttribute( "instance", InstanceController.getInstanceData( instance ) );
        
        if (queryString != null) {
            modelMap.addAttribute( "query", queryString );
            IngridQuery query = null;
            try {
                query = QueryStringParser.parse( queryString );
            } catch (TokenMgrError e) {
                return AdminViews.SE_INSTANCE_SEARCH;
            }

            // add instance information into query which is understood by this
            // iPlug
            query.put( "searchInInstances", new String[] { instance } );
            final IngridHits results = _plug.search( query, 0, 20 );
            modelMap.addAttribute( "totalHitCount", results.length() );

            final IngridHit[] hits = results.getHits();
            final IngridHitDetail[] details = _plug.getDetails( hits, query, new String[] {} );

            // convert details to map
            // this is necessary because it's not possible to access the
            // document-id by ${hit.documentId}
            final Map<String, IngridHitDetail> detailsMap = new HashMap<String, IngridHitDetail>();
            if (details != null) {
                for (final IngridHitDetail detail : details) {
                    detailsMap.put( detail.getDocumentId(), detail );
                }
            }

            modelMap.addAttribute( "hitCount", details.length );
            modelMap.addAttribute( "hits", detailsMap );
            modelMap.addAttribute( "details", _plug instanceof IRecordLoader );
        }

        return AdminViews.SE_INSTANCE_SEARCH;
    }

}
