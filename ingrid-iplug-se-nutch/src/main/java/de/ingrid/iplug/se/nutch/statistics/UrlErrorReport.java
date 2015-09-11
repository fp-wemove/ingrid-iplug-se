/*
 * **************************************************-
 * ingrid-iplug-se-nutch
 * ==================================================
 * Copyright (C) 2014 - 2015 wemove digital solutions GmbH
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
package de.ingrid.iplug.se.nutch.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.CrawlDb;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.protocol.ProtocolStatus;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.util.NutchJob;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author joachim
 * 
 *         Generates a report for all urls that had errors while fetching.
 *
 */
public class UrlErrorReport extends Configured implements Tool {

	private static final Log LOG = LogFactory.getLog(UrlErrorReport.class.getName());

	public static class UrlErrorStatusReportMapper implements Mapper<Text, CrawlDatum, Text, ProtocolStatus> {

		public void configure(JobConf job) {
		}

		public void close() {
		}

		public void map(Text key, CrawlDatum value, OutputCollector<Text, ProtocolStatus> output, Reporter reporter)
		        throws IOException {

			final Writable w = value.getMetaData().get(Nutch.WRITABLE_PROTO_STATUS_KEY);

			if (w != null && w instanceof ProtocolStatus) {
				final ProtocolStatus pStatus = (ProtocolStatus) w;
				int code = pStatus.getCode();
				switch (code) {
				case ProtocolStatus.ACCESS_DENIED:
				case ProtocolStatus.BLOCKED:
				case ProtocolStatus.EXCEPTION:
				case ProtocolStatus.FAILED:
				case ProtocolStatus.GONE:
				case ProtocolStatus.NOTFETCHING:
				case ProtocolStatus.NOTFOUND:
				case ProtocolStatus.PROTO_NOT_FOUND:
				case ProtocolStatus.ROBOTS_DENIED:
				case ProtocolStatus.REDIR_EXCEEDED:
				case ProtocolStatus.WOULDBLOCK:
					output.collect(key, pStatus);
					break;
				default:
					break;
				}
				return;
			}
		}
	}

	public UrlErrorReport(Configuration configuration) {
		super(configuration);
	}

	public UrlErrorReport() {
	}

	public void startUrlErrorReport(Path crawldb, Path outputDir) throws IOException {

		Path out = new Path(outputDir, "statistic/url_error_report");

		FileSystem fs = FileSystem.get(getConf());
		if (fs.exists(out)) {
			fs.delete(out, true);
		}

		LOG.info("Start url error report.");

		// filter out crawldb entries that match starturls
		JobConf job = new NutchJob(getConf());
		job.setJobName("starturlreport");

		FileInputFormat.addInputPath(job, new Path(crawldb, CrawlDb.CURRENT_NAME));
		job.setInputFormat(SequenceFileInputFormat.class);

		job.setMapperClass(UrlErrorStatusReportMapper.class);
		job.setOutputFormat(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(ProtocolStatus.class);

		String id = Integer.toString(new Random().nextInt(Integer.MAX_VALUE));
		String name = "url_error_report-temp-" + id;
		Path urlErrorReportEntries = new Path(getConf().get("hadoop.temp.dir", "."), name);
		FileOutputFormat.setOutputPath(job, urlErrorReportEntries);

		JobClient.runJob(job);

		SequenceFile.Reader reader = null;
		BufferedWriter br = null;
		try {

			reader = new SequenceFile.Reader(fs, new Path(urlErrorReportEntries, "part-00000"), getConf());
			Text key = new Text();
			ProtocolStatus value = new ProtocolStatus();

			Path file = new Path(out, "data.json");
			OutputStream os = fs.create(file);
			br = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			JSONArray array = new JSONArray();
			while (reader.next(key, value)) {

				JSONObject jsn = new JSONObject();
				jsn.put("url", key.toString());
				jsn.put("status", value.getCode());
				String msg = value.getName();
				if (value.getMessage() != null) {
					msg.concat(": " +  value.getMessage());
				}
				jsn.put("msg", msg);
				array.put(jsn);
			}
			br.write(array.toString());
		} catch (Exception e) {
			LOG.error("Error creating JSON from url error report", e);
			e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
			if (br != null) {
				br.close();
			}
		}

		fs.delete(urlErrorReportEntries, true);
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(NutchConfiguration.create(), new UrlErrorReport(), args);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: UrlErrorReport <crawldb> <output_dir>");
			System.err.println("       The statistic will be written to <output_dir>/statistic/url_error_report/data.json.");

			return -1;
		}
		try {
			startUrlErrorReport(new Path(args[0]), new Path(args[1]));
			return 0;
		} catch (Exception e) {
			LOG.error("UrlErrorReport: " + StringUtils.stringifyException(e));
			return -1;
		}
	}

}
