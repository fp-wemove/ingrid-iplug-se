package de.ingrid.iplug.se.urlmaintenance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.nutch.crawl.IPreCrawl;

import de.ingrid.iplug.se.urlmaintenance.persistence.dao.IDao;
import de.ingrid.iplug.se.urlmaintenance.persistence.model.Url;
// TODO rwe: cleanup code
public class UrlmaintenancePreCrawl implements IPreCrawl {

  private Configuration _conf;

  private static final Log LOG = LogFactory.getLog(UrlmaintenancePreCrawl.class);

  private FileSystem _fileSystem;
  private final String _lineSeparator;

  public UrlmaintenancePreCrawl() {
    super();
    _lineSeparator = System.getProperty("line.separator");
  }

  @Override
  public void preCrawl(Path crawlDir) throws IOException {
//    String tmp = System.getProperty("java.io.tmpdir");
//    File tmpFolder = new File(tmp, "portal-u-" + DatabaseExport.class.getName());

    String urlType = _conf.get("url.type");
//    File inDir = new File(tmpFolder, urlType);
//    File start = new File(inDir, "urls/start");
//    File limit = new File(inDir, "urls/limit");
//    File exclude = new File(inDir, "urls/exclude");
//    File metadata = new File(inDir, "urls/metadata");
//
//    // upload urls
//    upload(_fileSystem, start, crawlDir);
//    upload(_fileSystem, limit, crawlDir);
//    upload(_fileSystem, exclude, crawlDir);
//    upload(_fileSystem, metadata, crawlDir);

    DatabaseExport databaseExport = DatabaseExport.getInstance();

    if (databaseExport != null) {
      uploadUrlsFromDatabase(urlType, databaseExport, crawlDir);
    }
  }

  private void uploadUrlsFromDatabase(String urlType, DatabaseExport databaseExport, Path crawlDir) throws IOException {
    // TODO Auto-generated method stub
    LOG.info("Sync urls from database...");
    if(urlType.equals("web")){
      upload(databaseExport.getStartUrlDao(), "start", crawlDir);
      upload(databaseExport.getLimitUrlDao(), "limit", crawlDir);
      upload(databaseExport.getExcludeUrlDao(), "exclude", crawlDir);
      upload(databaseExport.getWebMetadataIterator(), "metadata", crawlDir);
    }
    else if(urlType.equals("catalog")){
      upload(databaseExport.getCatalogUrlDao(), "start", crawlDir);
      upload(databaseExport.getCatalogUrlDao(), "limit", crawlDir);
      upload(databaseExport.getCatalogMetadataIterator(), "metadata", crawlDir);
    }
    else{
      throw new IllegalArgumentException("Internal error: The given parameter 'urlType'='" +
      		urlType+"' is invalid.");
    }
    LOG.info("Sync urls from database... OK.");
  }

  @Override
  public Configuration getConf() {
    return _conf;
  }

  @Override
  public void setConf(Configuration conf) {
    _conf = conf;
    try {
      _fileSystem = FileSystem.get(conf);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void upload(FileSystem fileSystem, File urlFolder, Path crawlDir) throws IOException {
    if (!new File(urlFolder.getAbsolutePath(), "urls.txt").exists()) {
      return;
    }
    Path urlFile = new Path(urlFolder.getAbsolutePath(), "urls.txt");
    Path uploadPath = new Path(crawlDir, "urls/" + urlFolder.getName() + "/urls.txt");
    LOG.info("upload url file [" + urlFile + "] to hdfs [" + uploadPath + "]");
    fileSystem.copyFromLocalFile(false, true, urlFile, uploadPath);
  }

  private void upload(IDao<? extends Url> urlDao, String folderName, Path crawlDir) throws IOException {
    Path uploadPath = new Path(crawlDir, "urls/" + folderName + "/urls.txt");
    FSDataOutputStream dataOutputStream = _fileSystem.create(uploadPath, true);
    List<String> urls = new ArrayList<String>();
    
    for (Url url : urlDao.getAll()) {
      urls.add(url.getUrl());
    }
    upload(urls.iterator(), folderName, crawlDir);
    dataOutputStream.close();
  }

  private void upload(Iterator<String> dataIterator, String folderName, Path crawlDir) throws IOException {
    Path uploadPath = new Path(crawlDir, "urls/" + folderName + "/urls.txt");
    FSDataOutputStream dataOutputStream = _fileSystem.create(uploadPath, true);

    while(dataIterator.hasNext()) {
      String line = dataIterator.next() + _lineSeparator;
      dataOutputStream.writeBytes(line);
    }
    dataOutputStream.close();
  }
}
