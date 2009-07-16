package org.apache.nutch.crawl.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapFileOutputFormat;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.util.NutchJob;

public class ParseDataWrapper extends Configured {

  public static final Log LOG = LogFactory.getLog(ParseDataWrapper.class);

  public static class UrlParseDataContainer implements Writable {

    private Text _url = new Text();
    private ParseData _parseData = new ParseData();

    public UrlParseDataContainer() {
    }

    public UrlParseDataContainer(Text url, ParseData parseData) {
      _url = url;
      _parseData = parseData;
    }

    public Text getUrl() {
      return _url;
    }

    public void setUrl(Text url) {
      _url = url;
    }

    public ParseData getParseData() {
      return _parseData;
    }

    public void setParseData(ParseData parseData) {
      _parseData = parseData;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
      _url.readFields(in);
      _parseData = ParseData.read(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
      _url.write(out);
      _parseData.write(out);
    }

  }

  public static class ParseDataWrapperMapper implements
      Mapper<Text, ParseData, Text, UrlParseDataContainer> {

    @Override
    public void map(Text key, ParseData value,
        OutputCollector<Text, UrlParseDataContainer> collector,
        Reporter reporter) throws IOException {
      String url = key.toString();
      String host = new URL(url).getHost();
      UrlParseDataContainer container = new UrlParseDataContainer(
          new Text(url), value);
      collector.collect(new Text(host), container);
    }

    @Override
    public void configure(JobConf arg0) {

    }

    @Override
    public void close() throws IOException {

    }

  }

  public void wrap(Path segment, Path out) throws IOException {

    JobConf job = new NutchJob(getConf());
    job.setJobName("wrap segment: " + segment);

    job.setInputFormat(SequenceFileInputFormat.class);

    FileInputFormat.addInputPath(job, new Path(segment, ParseData.DIR_NAME));

    job.setMapperClass(ParseDataWrapperMapper.class);

    FileOutputFormat.setOutputPath(job, out);
    job.setOutputFormat(MapFileOutputFormat.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(UrlParseDataContainer.class);
    JobClient.runJob(job);
  }

  public static void main(String[] args) throws IOException {
    ParseDataWrapper wrapper = new ParseDataWrapper();
    wrapper.setConf(NutchConfiguration.create());
    wrapper.wrap(new Path(args[0]), new Path(args[1]));
  }
}
