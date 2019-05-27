/*
 * **************************************************-
 * ingrid-iplug-se-iplug
 * ==================================================
 * Copyright (C) 2014 - 2019 wemove digital solutions GmbH
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
package de.ingrid.iplug.se.webapp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import de.ingrid.admin.JettyStarter;
import de.ingrid.admin.command.PlugdescriptionCommandObject;
import de.ingrid.admin.controller.AbstractController;
import de.ingrid.admin.elasticsearch.IndexManager;
import de.ingrid.iplug.se.Configuration;
import de.ingrid.iplug.se.SEIPlug;
import de.ingrid.iplug.se.db.model.Url;
import de.ingrid.iplug.se.nutchController.NutchController;
import de.ingrid.iplug.se.utils.DBUtils;
import de.ingrid.iplug.se.utils.FileUtils;
import de.ingrid.iplug.se.webapp.container.Instance;
import de.ingrid.iplug.se.webapp.controller.instance.InstanceController;
import de.ingrid.iplug.se.webapp.controller.instance.scheduler.SchedulerManager;

/**
 * Control the database parameter page.
 * 
 * @author joachim@wemove.com
 * 
 */
@Controller
@SessionAttributes("plugDescription")
public class ListInstancesController extends AbstractController {
    
    private static Logger log = Logger.getLogger( ListInstancesController.class );

    

    @Autowired
    private SchedulerManager schedulerManager;

    @Autowired
    private NutchController nutchController;
    
    @Autowired
    private IndexManager indexManager;

    private Configuration conf;

    public ListInstancesController() {
        this.conf = SEIPlug.conf;
    }

    public ListInstancesController(Configuration conf) {
        this.conf = conf;
    }

    public List<Instance> getInstances() throws Exception {
        ArrayList<Instance> list = new ArrayList<Instance>();

        File[] instancesDirs = FileUtils.getInstancesDirs();
        for (File subDir : instancesDirs) {
            Instance instance = InstanceController.getInstanceData( subDir.getName() );
            list.add( instance );
        }

        return list;
    }

    @RequestMapping(value = { "/iplug-pages/listInstances.html" }, method = RequestMethod.GET)
    public String getParameters(final ModelMap modelMap) throws Exception {

        List<Instance> instances = getInstances();

        // check for invalid instances and remove them from the active ones
        Iterator<String> activeInstancesIt = JettyStarter.getInstance().config.indexSearchInTypes.iterator();
        while (activeInstancesIt.hasNext()) {
            String active = activeInstancesIt.next();

            boolean found = false;
            for (Instance instance : instances) {
                if (instance.getName().equals( active )) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                activeInstancesIt.remove();
            }
        }

        modelMap.put( "instances", instances );
        return AdminViews.SE_LIST_INSTANCES;
    }

    @RequestMapping(value = "/iplug-pages/listInstances.html", method = RequestMethod.POST)
    public String post(@ModelAttribute("plugDescription") final PlugdescriptionCommandObject pdCommandObject,
            @RequestParam(value = "action", required = false) final String action) throws Exception {

        return AdminViews.SAVE;
    }

    @RequestMapping(value = "/iplug-pages/listInstances.html", method = RequestMethod.POST, params = "add")
    public String addInstance(final ModelMap modelMap,
            @RequestParam("instance") String name,
            @RequestParam(value = "duplicateFrom", required = false) String from) throws Exception {
        
        if (name == null || name.isEmpty()) {
            modelMap.put( "instances", getInstances() );
            return AdminViews.SE_LIST_INSTANCES;
        }

        // convert illegal chars to "_"
        name = name.replaceAll( "[:\\\\/*?|<>\\W]", "_" );
        String dir = conf.getInstancesDir();

        // convert illegal chars to "_"
        name = name.replaceAll( "[:\\\\/*?|<>\\W]", "_" );
        // create directory and copy necessary configuration files
        boolean success = true;
        
        if (from == null) {
            success = initializeInstanceDir( dir + "/" + name );
            
        } else {
            success = success && copyUrlsFromInstanceTo( from, name ) && copyInstanceDir( dir + "/" + from, dir + "/" + name );
        }
        
        if (success) {
            schedulerManager.addInstance( name );
            modelMap.put( "instances", getInstances() );
            // modelMap.put( "error", "Index already exists for instance " + name + ", which might already contain data." );

            return AdminViews.SE_LIST_INSTANCES;

        } else {
            if (from == null) {
                modelMap.put( "error", "Default configuration could not be copied to: " + dir + "/" + name );
            } else {
                modelMap.put( "error", "Default configuration and/or URLs in database could not be copied to: " + dir + "/" + name );
            }
        }

        modelMap.put( "instances", getInstances() );

        return AdminViews.SE_LIST_INSTANCES;
    }

    private boolean copyUrlsFromInstanceTo(String from, String name) {
        
        try {
            List<Url> fromUrls = DBUtils.getAllUrlsFromInstance( from );
            log.debug( "Copy Urls: " + fromUrls.size() );
            
            // reset the IDs and set the name of the new instance
            for (Url url : fromUrls) {
                url.setId( null );
                url.setInstance( name );
            }
            
            DBUtils.addUrls( fromUrls );
        } catch (Exception e) { 
            log.error( "Error during duplication of URLs", e );
            e.printStackTrace();
            return false;
        }
        
        return true;
    }

    public static boolean initializeInstanceDir(String path) {
        boolean result = false;

        try {
            final Path newInstanceDir = Files.createDirectories( Paths.get( path ) );
            if (newInstanceDir == null) {
                throw new RuntimeException( "Directory could not be created: " + path );
            }

            // copy nutch configurations
            Path destDir = Paths.get( newInstanceDir.toString(), "conf" );
            Path sourceDir = Paths.get( "apache-nutch-runtime", "runtime", "local", "conf" );
            try {
                FileUtils.copyDirectories( sourceDir, destDir );
            } catch (IOException e) {
                e.printStackTrace();
                // modelMap.put( "error",
                // "Default configuration could not be copied to: " + destDir );
            }

            // copy default configuration
            destDir = Paths.get( newInstanceDir.toString(), "conf" );
            ClassPathResource instanceResourcesDir = new ClassPathResource( "instance-data" );
            sourceDir = Paths.get( instanceResourcesDir.getFile().getPath() );

            try {
                FileUtils.copyDirectories( sourceDir, destDir );
            } catch (IOException e) {
                e.printStackTrace();
                // modelMap.put( "error",
                // "Default configuration could not be copied to: " + destDir );
            }

            result = true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }
    
    private boolean copyInstanceDir(String from, String to) {
        Path sourceDir = Paths.get( from, "conf" );
        Path destDir = Paths.get( to, "conf" );
        
        try {
            final Path newInstanceDir = Files.createDirectories( destDir );
            if (newInstanceDir == null) {
                throw new RuntimeException( "Directory could not be created: " + destDir );
            }
            
            FileUtils.copyDirectories( sourceDir, destDir );
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    
    
    public void setSchedulerManager(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

}
