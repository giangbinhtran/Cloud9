/*
 * Cloud9: A MapReduce Library for Hadoop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.collection.medline;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.Counters.Counter;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * <p>
 * Program that builds the mapping from MEDLINE docids (PMIDs) to docnos
 * (sequentially-numbered ints). The program takes four command-line arguments:
 * </p>
 * 
 * <ul>
 * <li>[input] path to the document collection
 * <li>[output-dir] path to temporary MapReduce output directory
 * <li>[output-file] path to location of mappings file
 * <li>[num-mappers] number of mappers to run
 * </ul>
 * 
 * <p>
 * Here's a sample invocation:
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * hadoop jar cloud9.jar edu.umd.cloud9.collection.medline.NumberMedlineCitations \
 * /umd/collections/medline04.raw/ \
 * /user/jimmylin/medline-docid-tmp \
 * /user/jimmylin/docno.mapping 100
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Jimmy Lin
 */
public class NumberMedlineCitations extends Configured implements Tool {

	private static final Logger sLogger = Logger.getLogger(NumberMedlineCitations.class);

	private static enum Citations {
		TOTAL
	};

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, MedlineCitation, IntWritable, IntWritable> {

		private final static IntWritable ONE = new IntWritable(1);

		private final IntWritable sPmid = new IntWritable();

		public void map(LongWritable key, MedlineCitation citation,
				OutputCollector<IntWritable, IntWritable> output, Reporter reporter)
				throws IOException {

			sPmid.set(Integer.parseInt(citation.getPmid()));
			output.collect(sPmid, ONE);
			reporter.incrCounter(Citations.TOTAL, 1);
		}
	}

	private static class MyReducer extends MapReduceBase implements
			Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

		private final static IntWritable sCnt = new IntWritable(1);

		public void reduce(IntWritable key, Iterator<IntWritable> values,
				OutputCollector<IntWritable, IntWritable> output, Reporter reporter)
				throws IOException {
			output.collect(key, sCnt);
			sCnt.set(sCnt.get() + 1);
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public NumberMedlineCitations() {
	}

	private static int printUsage() {
		System.out.println("usage: [input-path] [output-path] [output-file] [num-mappers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}
	
	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 4) {
			printUsage();
			return -1;
		}
		
		String inputPath = args[0];
		String outputPath = args[1];
		String outputFile = args[2];
		int mapTasks = Integer.parseInt(args[3]);

		sLogger.info("Tool name: NumberMedlineCitations");
		sLogger.info(" - Input path: " + inputPath);
		sLogger.info(" - Output path: " + outputPath);
		sLogger.info(" - Output file: " + outputFile);
		sLogger.info("Launching with " + mapTasks + " mappers...");

		JobConf conf = new JobConf(getConf(), NumberMedlineCitations.class);
		conf.setJobName("NumberMedlineCitations");

		conf.setNumMapTasks(mapTasks);
		conf.setNumReduceTasks(1);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));
		FileOutputFormat.setCompressOutput(conf, false);

		conf.setInputFormat(MedlineCitationInputFormat.class);
		conf.setOutputKeyClass(IntWritable.class);
		conf.setOutputValueClass(IntWritable.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setReducerClass(MyReducer.class);

		// delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		RunningJob job = JobClient.runJob(conf);

		// write out various properties
		Counters counters = job.getCounters();
		Counter counter = counters.findCounter(
				"edu.umd.cloud9.collection.medline.NumberMedlineCitations$Citations", 0, "");

		int numdocs = (int) counter.getCounter();
		sLogger.info("total number of docs: " + numdocs);

		MedlineDocnoMapping.writeDocidData(outputPath + "/part-00000", outputFile);
		
		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NumberMedlineCitations(), args);
		System.exit(res);
	}
}
