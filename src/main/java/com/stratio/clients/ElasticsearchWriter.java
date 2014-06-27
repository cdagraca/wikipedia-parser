package com.stratio.clients;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xml.sax.SAXException;

import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;

/**
 * Created by luca on 12/06/14.
 */
public class ElasticsearchWriter implements Closeable {

	private static Logger logger = Logger.getLogger(ElasticsearchWriter.class);

	private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);

	private static String hosts;
	private static String cluster;
	private static String index;
	private static String type;
	private static int numShards;
	private static int numReplicas;
	private static int refreshSeconds;
	private static int batchSize;
	private static int skipFirstN;
	private static int skipLastN;
	private static int numDocs = 0;
	private static int numStatement = 0;

	private TransportClient client;

	private BulkRequestBuilder bulkRequest;

	public ElasticsearchWriter() {

		Settings settings = ImmutableSettings.settingsBuilder()
		                                     .put("client.transport.sniff", true)
		                                     .put("client.transport.nodes_sampler_interval", 60)
		                                     .put("cluster.name", cluster)
		                                     .build();		
		client = new TransportClient(settings);
		for (String host : hosts.split(",")) {
			client.addTransportAddress(new InetSocketTransportAddress(host, 9300));
		}
		client.connectedNodes();

		try {
			XContentBuilder json = jsonBuilder().startObject()
			                                    .startObject("settings")
			                                    .field("number_of_shards", numShards)
			                                    .field("number_of_replicas", numReplicas)
			                                    .field("refresh_interval", refreshSeconds)
			                                    .endObject()
			                                    .startObject("mappings")
			                                    .startObject(type)
			                                    .startObject("_source")
			                                    .field("enabled", true)
			                                    .endObject()
			                                    .startObject("properties")
			                                    .startObject("revision_id")
			                                    .field("store", false)
			                                    .field("type", "integer")
			                                    .endObject()
			                                    .startObject("revision_timestamp")
			                                    .field("store", false)
			                                    .field("type", "date")
			                                    .field("format", DATE_PATTERN)
			                                    .endObject()
			                                    .startObject("page_id")
			                                    .field("store", false)
			                                    .field("type", "integer")
			                                    .endObject()
			                                    .startObject("page_ns")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "not_analyzed")
			                                    .startObject("norms")
			                                    .field("enabled", false)
			                                    .endObject()
			                                    .endObject()
			                                    .startObject("page_fulltitle")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "analyzed")
			                                    .field("analyzer", "english")
			                                    .endObject()
			                                    .startObject("page_title")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "analyzed")
			                                    .field("analyzer", "english")
			                                    .endObject()
			                                    .startObject("page_restrictions")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "not_analyzed")
			                                    .startObject("norms")
			                                    .field("enabled", false)
			                                    .endObject()
			                                    .endObject()
			                                    .startObject("page_isredirect")
			                                    .field("store", false)
			                                    .field("type", "boolean")
			                                    .endObject()
			                                    .startObject("contributor_id")
			                                    .field("store", false)
			                                    .field("type", "integer")
			                                    .endObject()
			                                    .startObject("contributor_username")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "not_analyzed")
			                                    .startObject("norms")
			                                    .field("enabled", false)
			                                    .endObject()
			                                    .endObject()
			                                    .startObject("contributor_isanonymous")
			                                    .field("store", false)
			                                    .field("type", "boolean")
			                                    .endObject()
			                                    .startObject("revision_isminor")
			                                    .field("store", false)
			                                    .field("type", "boolean")
			                                    .endObject()
			                                    .startObject("revision_redirection")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "not_analyzed")
			                                    .startObject("norms")
			                                    .field("enabled", false)
			                                    .endObject()
			                                    .endObject()
			                                    .startObject("revision_text")
			                                    .field("store", false)
			                                    .field("type", "string")
			                                    .field("index", "analyzed")
			                                    .field("analyzer", "english")
			                                    .endObject()
			                                    .endObject()
			                                    .endObject()
			                                    .endObject()
			                                    .endObject();
			CreateIndexResponse response = client.admin()
			                                     .indices()
			                                     .prepareCreate(index)
			                                     .setSource(json)
			                                     .execute()
			                                     .actionGet();
			logger.info("Created index: " + response.isAcknowledged() + " " + response.toString());
		} catch (Exception e) {
			logger.info("Skip index creation");
		}

		bulkRequest = client.prepareBulk();
	}

	public void write(Revision r) {
		try {

			XContentBuilder json = jsonBuilder().startObject()
			                                    .field("revision_id", r.getId())
			                                    .field("revision_timestamp", sdf.format(r.getTimestamp()))
			                                    .field("page_id", r.getPage().getId())
			                                    .field("page_ns", r.getPage().getNamespace())
			                                    .field("page_fulltitle", r.getPage().getFullTitle())
			                                    .field("page_title", r.getPage().getTitle())
			                                    .field("page_restrictions", r.getPage().getRestrictions())
			                                    .field("page_isredirect", r.getPage().isRedirect())
			                                    .field("contributor_id", r.getContributor().getId())
			                                    .field("contributor_username", r.getContributor().getUsername())
			                                    .field("contributor_isanonymous", r.getContributor().getIsAnonymous())
			                                    .field("revision_isminor", r.isMinor())
			                                    .field("revision_redirection", r.getRedirection())
			                                    .field("revision_text", r.getText())
			                                    .endObject();

			IndexRequest indexRequest = client.prepareIndex(index, type).setSource(json).request();
			String upsertKey = new com.eaio.uuid.UUID().toString();
			UpdateRequest updateRequest = client.prepareUpdate()
			                                    .setIndex(index)
			                                    .setType(type)
			                                    .setUpsert(indexRequest)
			                                    .setSource(json)
			                                    .setId(upsertKey)
			                                    .setDoc(json)
			                                    .request();
			bulkRequest.add(updateRequest);

			numStatement++;
			numDocs++;

			if (numStatement == batchSize) {
				try {
					BulkResponse bulkResponse = bulkRequest.execute().actionGet();
					logger.info("Insertion Elasticsearch: " + bulkResponse.getTookInMillis() + "ms - " + numStatement
					        + " - " + numDocs);
				} catch (Exception e) {
					logger.error("Error while executing batch", e);
				}
				bulkRequest = client.prepareBulk();
				numStatement = 0;
			}

		} catch (Exception e) {
			logger.error("Cannot parse/insert revision with revision_id: " + r.getId(), e);

			return;
		}

	}

	public void close() {
		client.close();
	}

	public static void main(String[] args) throws IOException, SAXException {

		if (args.length != 11) {
			logger.warn("Usage: <input_file> <hosts> <cluste> <index> <type> <num_shards> <num_replicas> <refresh_seconds> <batch_size> <skip_first_n> <skip_last_n>");
			System.exit(1);
		}

		logger.info("Parsing file: " + args[0]);
		XMLDumpParser parser = new XMLDumpParser(args[0]);

		hosts = args[1];
		cluster = args[2];
		index = args[3];
		type = args[4];
		numShards = Integer.parseInt(args[5]);
		numReplicas = Integer.parseInt(args[6]);
		refreshSeconds = Integer.parseInt(args[7]);
		batchSize = Integer.parseInt(args[8]);
		skipFirstN = Integer.parseInt(args[9]);
		skipLastN = Integer.parseInt(args[10]);
		numDocs = skipFirstN;

		logger.info("Connecting to " + hosts + " " + index + " " + type);

		logger.info("Skipping first " + skipFirstN + " revisions");

		try (final ElasticsearchWriter writer = new ElasticsearchWriter()) {
			parser.getContentHandler().setPageCallback(new PageCallback() {
				@Override
				public void callback(Page page) {
					// logger.info(page);
				}
			});
			parser.getContentHandler().setRevisionCallback(new RevisionCallback() {
				private boolean started = false;
				private int revCounter = 0;

				@Override
				public void callback(Revision revision) {
					revCounter++;
					if (numDocs >= skipLastN) {
						logger.info("Insertion finished with " + numDocs);
						System.exit(1);
					}
					if (revCounter > skipFirstN) {

						if (!started) {
							logger.info("Skip finished. Start writing");
						}
						started = true;
						writer.write(revision);
					}
				}
			});

			logger.info("Starting parser");
			parser.parse();
		}

	}
}
