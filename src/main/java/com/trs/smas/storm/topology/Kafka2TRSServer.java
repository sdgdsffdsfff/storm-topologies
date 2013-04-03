package com.trs.smas.storm.topology;

import java.util.Properties;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;

import com.trs.smas.storm.bolt.Load2TRSServerBolt;
import com.trs.smas.storm.bolt.ParseCSVBolt;
import com.trs.smas.storm.spout.KafkaSpout;
import com.trs.smas.storm.util.PropertiesUtil;

public class Kafka2TRSServer {

	private static final String PROPS_FILE = "kafka2trsserver.properties";

	public static void main(String[] args) throws Exception {
		Properties props = PropertiesUtil.loadPropertiesFromClasspath(PROPS_FILE);

		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout("spout",
				new KafkaSpout(props, props.getProperty("topic")), 2);
		builder.setBolt("parse", new ParseCSVBolt(), 2)
				.shuffleGrouping("spout");
		// builder.setBolt("distinguish", new
		// DistinguishBolt(),3).shuffleGrouping("parse");
		builder.setBolt(
				"trsserver",
				new Load2TRSServerBolt(props.getProperty("trsserver.host"),
						props.getProperty("trsserver.port"), 
						props.getProperty("trsserver.username"), 
						props.getProperty("trsserver.password"), 
						props.getProperty("trsserver.database"),
						props.getProperty("trsserver.fields")), 4)
				.shuffleGrouping("parse");

		Config conf = new Config();

		if (args != null && args.length > 0) {
			conf.setNumWorkers(3);

			StormSubmitter.submitTopology(args[0], conf,
					builder.createTopology());
		} else {
			conf.setMaxTaskParallelism(3);

			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology("kafa-to-trsserver", conf,
					builder.createTopology());

			Thread.sleep(1000 * 100);

			cluster.shutdown();
		}
	}
}
