/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.crawl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapFileOutputFormat;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.net.URLFilterException;
import org.apache.nutch.net.URLFilters;
import org.apache.nutch.net.URLNormalizers;
import org.apache.nutch.scoring.ScoringFilterException;
import org.apache.nutch.scoring.ScoringFilters;
import org.apache.nutch.util.LockUtil;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.util.NutchJob;
import org.apache.nutch.util.SyncUtil;

/** Generates a subset of a crawl db to fetch. */
public class Generator extends Configured implements Tool {

  public static final String CRAWL_GENERATE_FILTER = "crawl.generate.filter";
  public static final String GENERATE_MAX_PER_HOST_BY_IP = "generate.max.per.host.by.ip";
  public static final String GENERATE_MAX_PER_HOST = "generate.max.per.host";
  public static final String GENERATE_UPDATE_CRAWLDB = "generate.update.crawldb";
  public static final String CRAWL_TOP_N = "crawl.topN";
  public static final String CRAWL_GEN_CUR_TIME = "crawl.gen.curTime";
  public static final String CRAWL_GEN_DELAY = "crawl.gen.delay";
  public static final Log LOG = LogFactory.getLog(Generator.class);
  
  public static class SelectorEntry implements Writable {
    public Text url;
    public CrawlDatum datum;
    
    public SelectorEntry() {
      url = new Text();
      datum = new CrawlDatum();
    }

    public void readFields(DataInput in) throws IOException {
      url.readFields(in);
      datum.readFields(in);
    }

    public void write(DataOutput out) throws IOException {
      url.write(out);
      datum.write(out);
    }
    
    public String toString() {
      return "url=" + url.toString() + ", datum=" + datum.toString();
    }
  }

  /** Selects entries due for fetch. */
  public static class Selector implements Mapper<Text, CrawlDatum, FloatWritable, SelectorEntry>, Partitioner<FloatWritable, Writable>, Reducer<FloatWritable, SelectorEntry, FloatWritable, SelectorEntry> {
    private LongWritable genTime = new LongWritable(System.currentTimeMillis());
    private long curTime;
    private long limit;
    private long count;
    private HashMap<String, IntWritable> hostCounts =
      new HashMap<String, IntWritable>();
    private int maxPerHost;
    private HashSet<String> maxedHosts = new HashSet<String>();
    private HashSet<String> dnsFailureHosts = new HashSet<String>();
    private Partitioner<Text, Writable> hostPartitioner = new PartitionUrlByHost();
    private URLFilters filters;
    private URLNormalizers normalizers;
    private ScoringFilters scfilters;
    private SelectorEntry entry = new SelectorEntry();
    private FloatWritable sortValue = new FloatWritable();
    private boolean byIP;
    private long dnsFailure = 0L;
    private boolean filter;
    private long genDelay;
    private FetchSchedule schedule;

    public void configure(JobConf job) {
      curTime = job.getLong(CRAWL_GEN_CUR_TIME, System.currentTimeMillis());
      limit = job.getLong(CRAWL_TOP_N,Long.MAX_VALUE)/job.getNumReduceTasks();
      maxPerHost = job.getInt(GENERATE_MAX_PER_HOST, -1);
      byIP = job.getBoolean(GENERATE_MAX_PER_HOST_BY_IP, false);
      filters = new URLFilters(job);
      normalizers = new URLNormalizers(job, URLNormalizers.SCOPE_GENERATE_HOST_COUNT);
      scfilters = new ScoringFilters(job);
      hostPartitioner.configure(job);
      filter = job.getBoolean(CRAWL_GENERATE_FILTER, true);
      genDelay = job.getLong(CRAWL_GEN_DELAY, 7L) * 3600L * 24L * 1000L;
      long time = job.getLong(Nutch.GENERATE_TIME_KEY, 0L);
      if (time > 0) genTime.set(time);
      schedule = FetchScheduleFactory.getFetchSchedule(job);
    }

    public void close() {}

    /** Select & invert subset due for fetch. */
    public void map(Text key, CrawlDatum value,
                    OutputCollector<FloatWritable, SelectorEntry> output, Reporter reporter)
      throws IOException {
      Text url = key;
      if (filter) {
        // If filtering is on don't generate URLs that don't pass URLFilters
        try {
          if (filters.filter(url.toString()) == null)
            return;
        } catch (URLFilterException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Couldn't filter url: " + url + " (" + e.getMessage()
                + ")");
          }
        }
      }
      CrawlDatum crawlDatum = value;

      // check fetch schedule
      if (!schedule.shouldFetch(url, crawlDatum, curTime)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("-shouldFetch rejected '" + url+ "', fetchTime=" + crawlDatum.getFetchTime() + ", curTime=" + curTime);
        }
        return;
      }
      if (LOG.isDebugEnabled()) {
          LOG.debug("-shouldFetch accepted '" + url+ "', fetchTime=" + crawlDatum.getFetchTime() + ", curTime=" + curTime);
        }

      LongWritable oldGenTime = (LongWritable)crawlDatum.getMetaData().get(Nutch.WRITABLE_GENERATE_TIME_KEY);
      if (oldGenTime != null) { // awaiting fetch & update
        if (oldGenTime.get() + genDelay > curTime) // still wait for update
          return;
      }
      float sort = 1.0f;
      try {
        sort = scfilters.generatorSortValue((Text)key, crawlDatum, sort);
      } catch (ScoringFilterException sfe) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Couldn't filter generatorSortValue for " + key + ": " + sfe);
        }
      }
      // sort by decreasing score, using DecreasingFloatComparator
      sortValue.set(sort);
      // record generation time
      crawlDatum.getMetaData().put(Nutch.WRITABLE_GENERATE_TIME_KEY, genTime);
      entry.datum = crawlDatum;
      entry.url = (Text)key;
      output.collect(sortValue, entry);          // invert for sort by score
    }

    /** Partition by host. */
    public int getPartition(FloatWritable key, Writable value,
                            int numReduceTasks) {
      return hostPartitioner.getPartition(((SelectorEntry)value).url, key,
                                          numReduceTasks);
    }

    /** Collect until limit is reached. */
    public void reduce(FloatWritable key, Iterator<SelectorEntry> values,
                       OutputCollector<FloatWritable, SelectorEntry> output,
                       Reporter reporter)
      throws IOException {

      while (values.hasNext() && count < limit) {

        SelectorEntry entry = values.next();
        Text url = entry.url;        
        String urlString = url.toString();        
        URL u = null;
        
        // skip bad urls, including empty and null urls
        try {
          u = new URL(url.toString());
        } catch (MalformedURLException e) {
          LOG.info("Bad protocol in url: " + url.toString());
          continue;
        }
        
        String host = u.getHost();
        host = host.toLowerCase();
        String hostname = host;

        // partitioning by ip will generate lots of DNS requests here, and will 
        // be up to double the overall dns load, do not run this way unless you
        // are running a local caching DNS server or a two layer DNS cache
        if (byIP) {
          if (maxedHosts.contains(host)) {
            if (LOG.isDebugEnabled()) { LOG.debug("Host already maxed out: " + host); }
            continue;
          }
          if (dnsFailureHosts.contains(host)) {
            if (LOG.isDebugEnabled()) { LOG.debug("Host name lookup already failed: " + host); }
            continue;
          }
          try {
            InetAddress ia = InetAddress.getByName(host);
            host = ia.getHostAddress();
            urlString = new URL(u.getProtocol(), host, u.getPort(), u.getFile()).toString();
          } 
          catch (UnknownHostException uhe) {
            // remember hostnames that could not be looked up
            dnsFailureHosts.add(hostname);
            if (LOG.isDebugEnabled()) {
              LOG.debug("DNS lookup failed: " + host + ", skipping.");
            }
            dnsFailure++;
            if ((dnsFailure % 1000 == 0) && (LOG.isWarnEnabled())) {
              LOG.warn("DNS failures: " + dnsFailure);
            }
            continue;
          }
        }
        
        try {
          urlString = normalizers.normalize(urlString, URLNormalizers.SCOPE_GENERATE_HOST_COUNT);
          host = new URL(urlString).getHost();
        } catch (Exception e) {
          LOG.warn("Malformed URL: '" + urlString + "', skipping (" +
              StringUtils.stringifyException(e) + ")");
          continue;
        }
        
        // only filter if we are counting hosts
        if (maxPerHost > 0) {
          
          IntWritable hostCount = hostCounts.get(host);
          if (hostCount == null) {
            hostCount = new IntWritable();
            hostCounts.put(host, hostCount);
          }
  
          // increment hostCount
          hostCount.set(hostCount.get() + 1);
  
          // skip URL if above the limit per host.
          if (hostCount.get() > maxPerHost) {
            if (hostCount.get() == maxPerHost + 1) {
              // remember the raw hostname that is maxed out
              maxedHosts.add(hostname);
              if (LOG.isInfoEnabled()) {
                LOG.info("Host " + host + " has more than " + maxPerHost +
                         " URLs." + " Skipping additional.");
              }
            }
            continue;
          }
        }

        output.collect(key, entry);

        // Count is incremented only when we keep the URL
        // maxPerHost may cause us to skip it.
        count++;
      }
    }
  }

  public static class DecreasingFloatComparator extends FloatWritable.Comparator {

    /** Compares two FloatWritables decreasing. */
    public int compare(byte[] b1, int s1, int l1,
        byte[] b2, int s2, int l2) {
      return super.compare(b2, s2, l2, b1, s1, l1);
    }
  }

  public static class SelectorInverseMapper extends MapReduceBase implements Mapper<FloatWritable, SelectorEntry, Text, SelectorEntry> {

    public void map(FloatWritable key, SelectorEntry value, OutputCollector<Text, SelectorEntry> output, Reporter reporter) throws IOException {
      SelectorEntry entry = (SelectorEntry)value;
      output.collect(entry.url, entry);
    }
  }
  
  public static class PartitionReducer extends MapReduceBase
      implements Reducer<Text, SelectorEntry, Text, CrawlDatum> {

    public void reduce(Text key, Iterator<SelectorEntry> values,
        OutputCollector<Text, CrawlDatum> output, Reporter reporter) throws IOException {
      // if using HashComparator, we get only one input key in case of hash collision
      // so use only URLs from values
      while (values.hasNext()) {
        SelectorEntry entry = values.next();
        output.collect(entry.url, entry.datum);
      }
    }
    
  }

  /** Sort fetch lists by hash of URL. */
  public static class HashComparator extends WritableComparator {
    public HashComparator() {
      super(Text.class);
    }

    public int compare(WritableComparable a, WritableComparable b) {
      Text url1 = (Text) a;
      Text url2 = (Text) b;
      int hash1 = hash(url1.getBytes(), 0, url1.getLength());
      int hash2 = hash(url2.getBytes(), 0, url2.getLength());
      return (hash1 < hash2 ? -1 : (hash1 == hash2 ? 0 : 1));
    }

    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
      int hash1 = hash(b1, s1, l1);
      int hash2 = hash(b2, s2, l2);
      return (hash1 < hash2 ? -1 : (hash1 == hash2 ? 0 : 1));
    }

    private static int hash(byte[] bytes, int start, int length) {
      int hash = 1;
      // make later bytes more significant in hash code, so that sorting by
      // hashcode correlates less with by-host ordering.
      for (int i = length - 1; i >= 0; i--)
        hash = (31 * hash) + (int) bytes[start + i];
      return hash;
    }
  }

  /**
   * Update the CrawlDB so that the next generate won't include the same URLs.
   */
  public static class CrawlDbUpdater extends MapReduceBase implements Mapper<WritableComparable, Writable, Text, CrawlDatum>, Reducer<Text, CrawlDatum, Text, CrawlDatum> {
    long generateTime;
    
    public void configure(JobConf job) {
      generateTime = job.getLong(Nutch.GENERATE_TIME_KEY, 0L);
    }
    
    public void map(WritableComparable key, Writable value, OutputCollector<Text, CrawlDatum> output, Reporter reporter) throws IOException {
      if (key instanceof FloatWritable) { // tempDir source
        SelectorEntry se = (SelectorEntry)value;
        output.collect(se.url, se.datum);
      } else {
        output.collect((Text)key, (CrawlDatum)value);
      }
    }
    private CrawlDatum orig = new CrawlDatum();
    private LongWritable genTime = new LongWritable(0L);

    public void reduce(Text key, Iterator<CrawlDatum> values, OutputCollector<Text, CrawlDatum> output, Reporter reporter) throws IOException {
      while (values.hasNext()) {
        CrawlDatum val = values.next();
        if (val.getMetaData().containsKey(Nutch.WRITABLE_GENERATE_TIME_KEY)) {
          LongWritable gt = (LongWritable)val.getMetaData().get(Nutch.WRITABLE_GENERATE_TIME_KEY);
          genTime.set(gt.get());
          if (genTime.get() != generateTime) {
            orig.set(val);
            genTime.set(0L);
            continue;
          }
        } else {
          orig.set(val);
        }
      }
      if (genTime.get() != 0L) {
        orig.getMetaData().put(Nutch.WRITABLE_GENERATE_TIME_KEY, genTime);
      }
      output.collect(key, orig);
    }    
  }
  
  public Generator() {}
  
  public Generator(Configuration conf) {
    setConf(conf);
  }
  
  /**
   * Generate fetchlists in a segment. Whether to filter URLs or not is
   * read from the crawl.generate.filter property in the configuration
   * files. If the property is not found, the URLs are filtered.
   *
   * @param dbDir     Crawl database directory
   * @param segments  Segments directory
   * @param numLists  Number of reduce tasks
   * @param topN      Number of top URLs to be selected
   * @param curTime   Current time in milliseconds
   *
   * @return Path to generated segment or null if no entries were
   *         selected
   *
   * @throws IOException When an I/O error occurs
   */
  public Path generate(Path dbDir, Path segments, int numLists,
                       long topN, long curTime) throws IOException {

    JobConf job = new NutchJob(getConf());
    boolean filter = job.getBoolean(CRAWL_GENERATE_FILTER, true);
    return generate(dbDir, segments, numLists, topN, curTime, filter, false);
  }

  /**
   * Generate fetchlists in a segment.
   * @return Path to generated segment or null if no entries were selected.
   * */
  public Path generate(Path dbDir, Path segments,
                       int numLists, long topN, long curTime, boolean filter,
                       boolean force)
    throws IOException {

    Path tempDir =
      new Path(getConf().get("mapred.temp.dir", ".") +
               "/generate-temp-"+ System.currentTimeMillis());

    Path segment = new Path(segments, generateSegmentName());
    Path output = new Path(segment, CrawlDatum.GENERATE_DIR_NAME);
    
    Path lock = new Path(dbDir, CrawlDb.LOCK_NAME);
    FileSystem fs = FileSystem.get(getConf());
    LockUtil.createLockFile(fs, lock, force);

    LOG.info("Generator: Selecting best-scoring urls due for fetch.");
    LOG.info("Generator: starting");
    LOG.info("Generator: segment: " + segment);
    LOG.info("Generator: filtering: " + filter);
    if (topN != Long.MAX_VALUE) {
      LOG.info("Generator: topN: " + topN);
    }

    // map to inverted subset due for fetch, sort by score
    JobConf job = new NutchJob(getConf());
    job.setJobName("generate: select " + segment);

    if (numLists == -1) {                         // for politeness make
      numLists = job.getNumMapTasks();            // a partition per fetch task
    }
    if ("local".equals(job.get("mapred.job.tracker")) && numLists != 1) {
      // override
      LOG.info("Generator: jobtracker is 'local', generating exactly one partition.");
      numLists = 1;
    }
    job.setLong(CRAWL_GEN_CUR_TIME, curTime);
    // record real generation time
    long generateTime = System.currentTimeMillis();
    job.setLong(Nutch.GENERATE_TIME_KEY, generateTime);
    job.setLong(CRAWL_TOP_N, topN);
    job.setBoolean(CRAWL_GENERATE_FILTER, filter);

    FileInputFormat.addInputPath(job, new Path(dbDir, CrawlDb.CURRENT_NAME));
    job.setInputFormat(SequenceFileInputFormat.class);

    job.setMapperClass(Selector.class);
    job.setPartitionerClass(Selector.class);
    job.setReducerClass(Selector.class);

    FileOutputFormat.setOutputPath(job, tempDir);
    job.setOutputFormat(SequenceFileOutputFormat.class);
    job.setOutputKeyClass(FloatWritable.class);
    job.setOutputKeyComparatorClass(DecreasingFloatComparator.class);
    job.setOutputValueClass(SelectorEntry.class);
    try {
      //JobClient.runJob(job);    
      SyncUtil.syncJobRun(job);
    } catch (IOException e) {
      LockUtil.removeLockFile(fs, lock);
      throw e;
    }
    
    // check that we selected at least some entries ...
    SequenceFile.Reader[] readers = SequenceFileOutputFormat.getReaders(job, tempDir);
    boolean empty = true;
    if (readers != null && readers.length > 0) {
      for (int num = 0; num < readers.length; num++) {
        if (readers[num].next(new FloatWritable())) {
          empty = false;
          break;
        }
      }
    }
    
    for (int i = 0; i < readers.length; i++) readers[i].close();
    
    if (empty) {
      LOG.warn("Generator: 0 records selected for fetching, exiting ...");
      LockUtil.removeLockFile(fs, lock);
      fs.delete(tempDir, true);
      return null;
    }

    // invert again, paritition by host, sort by url hash
    if (LOG.isInfoEnabled()) {
      LOG.info("Generator: Partitioning selected urls by host, for politeness.");
    }
    job = new NutchJob(getConf());
    job.setJobName("generate: partition " + segment);
    
    job.setInt("partition.url.by.host.seed", new Random().nextInt());

    FileInputFormat.addInputPath(job, tempDir);
    job.setInputFormat(SequenceFileInputFormat.class);

    job.setMapperClass(SelectorInverseMapper.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(SelectorEntry.class);
    job.setPartitionerClass(PartitionUrlByHost.class);
    job.setReducerClass(PartitionReducer.class);
    job.setNumReduceTasks(numLists);

    FileOutputFormat.setOutputPath(job, output);
    job.setOutputFormat(SequenceFileOutputFormat.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(CrawlDatum.class);
    job.setOutputKeyComparatorClass(HashComparator.class);
    try {
        SyncUtil.syncJobRun(job);//JobClient.runJob(job);
    } catch (IOException e) {
      LockUtil.removeLockFile(fs, lock);
      fs.delete(tempDir, true);
      throw e;
    }
    if (getConf().getBoolean(GENERATE_UPDATE_CRAWLDB, false)) {
      // update the db from tempDir
      Path tempDir2 =
        new Path(getConf().get("mapred.temp.dir", ".") +
                 "/generate-temp-"+ System.currentTimeMillis());
  
      job = new NutchJob(getConf());
      job.setJobName("generate: updatedb " + dbDir);
      job.setLong(Nutch.GENERATE_TIME_KEY, generateTime);
      FileInputFormat.addInputPath(job, tempDir);
      FileInputFormat.addInputPath(job, new Path(dbDir, CrawlDb.CURRENT_NAME));
      job.setInputFormat(SequenceFileInputFormat.class);
      job.setMapperClass(CrawlDbUpdater.class);
      job.setReducerClass(CrawlDbUpdater.class);
      job.setOutputFormat(MapFileOutputFormat.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(CrawlDatum.class);
      FileOutputFormat.setOutputPath(job, tempDir2);
      try {
          SyncUtil.syncJobRun(job);//JobClient.runJob(job);
        CrawlDb.install(job, dbDir);
      } catch (IOException e) {
        LockUtil.removeLockFile(fs, lock);
        fs.delete(tempDir, true);
        fs.delete(tempDir2, true);
        throw e;
      }
      fs.delete(tempDir2, true);
    }
    LockUtil.removeLockFile(fs, lock);
    fs.delete(tempDir, true);

    if (LOG.isInfoEnabled()) { LOG.info("Generator: done."); }

    return segment;
  }
  
  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

  public static synchronized String generateSegmentName() {
    try {
      Thread.sleep(1000);
    } catch (Throwable t) {};
    return sdf.format
      (new Date(System.currentTimeMillis()));
  }

  /**
   * Generate a fetchlist from the crawldb.
   */
  public static void main(String args[]) throws Exception {
    int res = ToolRunner.run(NutchConfiguration.create(), new Generator(), args);
    System.exit(res);
  }
  
  public int run(String[] args) throws Exception {
    if (args.length < 2) {
      System.out.println("Usage: Generator <crawldb> <segments_dir> [-force] [-topN N] [-numFetchers numFetchers] [-adddays numDays] [-noFilter]");
      return -1;
    }

    Path dbDir = new Path(args[0]);
    Path segmentsDir = new Path(args[1]);
    long curTime = System.currentTimeMillis();
    long topN = Long.MAX_VALUE;
    int numFetchers = -1;
    boolean filter = true;
    boolean force = false;

    for (int i = 2; i < args.length; i++) {
      if ("-topN".equals(args[i])) {
        topN = Long.parseLong(args[i+1]);
        i++;
      } else if ("-numFetchers".equals(args[i])) {
        numFetchers = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-adddays".equals(args[i])) {
        long numDays = Integer.parseInt(args[i+1]);
        curTime += numDays * 1000L * 60 * 60 * 24;
      } else if ("-noFilter".equals(args[i])) {
        filter = false;
      } else if ("-force".equals(args[i])) {
        force = true;
      }
      
    }

    try {
      Path seg = generate(dbDir, segmentsDir, numFetchers, topN, curTime, filter, force);
      if (seg == null) return -2;
      else return 0;
    } catch (Exception e) {
      LOG.fatal("Generator: " + StringUtils.stringifyException(e));
      return -1;
    }
  }
}
