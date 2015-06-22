package eu.fbk.dkm.pikes.tintop.annotators.raw;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.jackson.map.ObjectMapper;
import org.fbk.cit.hlt.thewikimachine.util.CommandLineWithLogger;
import org.fbk.dkm.nlp.pipeline.PipelineConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class DBpediaSpotlight {

	static Logger logger = Logger.getLogger(DBpediaSpotlight.class.getName());
	static String urlAddress;
	private Properties config = new Properties();

	private String prefix = "";

	public DBpediaSpotlight() {
		this(PipelineConfiguration.getInstance().getProperties());
	}

	public DBpediaSpotlight(Properties properties) {
		config = properties;
		urlAddress = config.getProperty(prefix + "address");
	}

	public List<DBpediaSpotlightTag> tag(String text) throws Exception {

		String thisRequest = "";
		String fromServer = null;

		ArrayList<DBpediaSpotlightTag> ret = new ArrayList<>();

		Map<String, String> pars = new HashMap<>();
		pars.put("confidence", config.getProperty(prefix + "min_confidence"));
		pars.put("text", text);

		for (String key : pars.keySet()) {
			String value = pars.get(key);
			try {
				thisRequest += "&" + key + "=" + URLEncoder.encode(value, "utf-8");
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}

		URL serverAddress = new URL(urlAddress);
		logger.debug("URL: " + urlAddress);
		logger.debug("Request: " + thisRequest);

		boolean useProxy = config.getProperty(prefix + "use_proxy").equals("1");


		HttpURLConnection connection;

		if (useProxy) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProperty(prefix + "proxy_url"), Integer.parseInt(config.getProperty(prefix + "proxy_port"))));
			connection = (HttpURLConnection) serverAddress.openConnection(proxy);
		}
		else {
			connection = (HttpURLConnection) serverAddress.openConnection();
		}

		connection.setRequestMethod("POST");
		connection.setConnectTimeout(Integer.parseInt(config.getProperty(prefix + "timeout")));
		connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);
		connection.getOutputStream().write(thisRequest.getBytes(CharEncoding.UTF_8));
		connection.connect();

		// read the result from the server
		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder sb = new StringBuilder();
		while ((fromServer = rd.readLine()) != null) {
			sb.append(fromServer + '\n');
		}

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> userData = mapper.readValue(sb.toString(), Map.class);

		ArrayList<LinkedHashMap> annotation = (ArrayList<LinkedHashMap>) userData.get(new String("Resources"));
		if (annotation != null) {
			for (LinkedHashMap keyword : annotation) {
				String originalText = (String) keyword.get("@surfaceForm");
				DBpediaSpotlightTag tag = new DBpediaSpotlightTag(
						Integer.parseInt((String) keyword.get("@offset")),
						(String) keyword.get("@URI"),
						Double.parseDouble((String) keyword.get("@similarityScore")),
						originalText,
						originalText.length()
				);
				ret.add(tag);
//			System.out.println(tag);
			}
		}
//		System.out.println(annotation);
//		System.out.println(userData);

		return ret;
	}

	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

//    options.addOption(OptionBuilder.withDescription("Languages mappings").isRequired()
//            .hasArgs().withArgName("iso-codes").withLongOpt("languages").create("l"));
//    options.addOption(OptionBuilder.withDescription("Mapping folder").isRequired()
//            .hasArgs().withArgName("folder").withLongOpt("mapping-dir").create("f"));
//    options.addOption(OptionBuilder.withDescription("enter in the interactive mode")
//            .withLongOpt("interactive-mode").create("I"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		DBpediaSpotlight s = new DBpediaSpotlight();
		try {
			s.tag("The dinner is not scheduled.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}