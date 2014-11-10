/*
 * **************************************************-
 * ingrid-iplug-se-iplug
 * ==================================================
 * Copyright (C) 2014 wemove digital solutions GmbH
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
package de.ingrid.iplug.se.iplug;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ingrid.admin.JettyStarter;
import de.ingrid.admin.service.PlugDescriptionService;
import de.ingrid.iplug.se.SEIPlug;
import de.ingrid.iplug.se.elasticsearch.bean.ElasticsearchNodeFactoryBean;
import de.ingrid.utils.PlugDescription;

/**
 * Adds all fields in the index to the plugdescription AND configuration.
 * 
 * @author joachim
 *
 */
@Service
public class IPlugSEPostCrawlProcessor implements IPostCrawlProcessor {

    private static final Log LOG = LogFactory.getLog(IPlugSEPostCrawlProcessor.class);

    @Autowired
    private ElasticsearchNodeFactoryBean elasticSearch;

    @Autowired
    private PlugDescriptionService plugDescriptionService;

    @Override
    public void execute() {

        try {
            PlugDescription pd = plugDescriptionService.getPlugDescription();

            Client client = elasticSearch.getObject().client();
            ClusterState clusterState = client.admin().cluster().prepareState().execute().actionGet().getState();
            IndexMetaData inMetaData = clusterState.getMetaData().index(SEIPlug.conf.index);
            ImmutableOpenMap<String, MappingMetaData> metad = inMetaData.getMappings();
            @SuppressWarnings("unchecked")
            List<String> fields = pd.getArrayList(PlugDescription.FIELDS);
            if (fields == null) {
                fields = new ArrayList<String>();
                pd.put(PlugDescription.FIELDS, fields);
            }

            for (Iterator<MappingMetaData> i = metad.valuesIt(); i.hasNext();) {
                MappingMetaData mmd = i.next();
                Map<String, Object> src = (Map<String, Object>) mmd.getSourceAsMap().get("properties");
                for (String f : src.keySet()) {
                    if (!fields.contains(f)) {
                        pd.addField(f);
                    }
                }
            }
            if (!fields.contains("site")) {
                pd.addField("site");
            }

            plugDescriptionService.savePlugDescription(pd);
            JettyStarter.getInstance().config.writePlugdescriptionToProperties(plugDescriptionService.getCommandObect());

        } catch (Exception e) {
            LOG.error("Error adding index fields to plugdescription.", e);
        }

    }

}
