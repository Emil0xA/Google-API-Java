package gYouTube;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List; 

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics;
import com.google.api.services.youtubeAnalytics.model.ResultTable;
import com.google.api.services.youtubeAnalytics.model.ResultTable.ColumnHeaders;
import com.google.common.collect.Lists;

/**
 * A simple application that uses YouTube APIs
 * 
 * Display YouTube metrics from a user's channel.
 * 
 * Reference: 
 * https://developers.google.com/youtube/analytics/v1/code_samples/java
 * 
 * @author Emil Tan
 */
@SuppressWarnings("deprecation")
public class YouTubeAnalyticsReport {
	/** HTTP transport */
	private final HttpTransport httpTransport = new NetHttpTransport(); 
	/** JSON factory */
	private final JsonFactory jsonFactory = new JacksonFactory(); 

	/** To make YouTube API requests */
	private YouTube youtube; 

	/** To make analytic API requests */
	private YouTubeAnalytics analytics; 
	
	/** Channel to retrieve analytic data from */
	private String channelID; 
	
	/** To print out reports */
	private PrintStream writer = System.out; 
	
	/**
	 * Constructor - Acquire authorisation and user's channel information
	 * 
	 * @param clientID
	 * @throws IOException
	 */
	public YouTubeAnalyticsReport(String clientID, String clientSecret) throws IOException {
		Credential credential = authorise(clientID, clientSecret); 
		
		youtube = new YouTube.Builder(httpTransport, jsonFactory, credential).setApplicationName("youtube-analytics-api-report-example").build(); 
		analytics = new YouTubeAnalytics.Builder(httpTransport, jsonFactory, credential).setApplicationName("youtube-analytics-api-report-example").build(); 	
	
		YouTube.Channels.List channelRequest = youtube.channels().list("id,snippet"); 
		channelRequest.setMine(true); 
		channelRequest.setFields("items(id,snippet/title)");
		ChannelListResponse channels = channelRequest.execute(); 
		
		List<Channel> listOfChannels = channels.getItems(); 
		
		Channel defaultChannel = listOfChannels.get(0); 
		channelID = defaultChannel.getId(); 
		
		if(channelID == null) {
			writer.println("No channel found.");
		} else {
			writer.println("Default Channel: " + defaultChannel.getSnippet().getTitle() + " (" + channelID + ")");
			writer.println();
		}
	}

	/**
	 * Acquire authorisation to access user's protected YouTube data
	 * 
	 * @param clientID OAuth Client ID
	 * @param clientSecret OAuth Client Secret 
	 * @throws IOException 
	 */
	private Credential authorise(String clientID, String clientSecret) throws IOException {
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/yt-analytics.readonly", "https://www.googleapis.com/auth/youtube.readonly"); 
		
		/* Set up file credential store */
		FileCredentialStore credentialStore = new FileCredentialStore(new File(System.getProperty("user.home"), ".credentials/youtube-analytics.api-report.json"), jsonFactory);
		
		/* Set up authorisation code flow */
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientID, clientSecret, scopes).setCredentialStore(credentialStore).build(); 
		
		/* Authorise */
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user"); 
	}
	
	/**
	 * Top videos by views 
	 * 
	 * @param analytics Analytics service used to access the API
	 * @param id ID from which to retrieve data
	 * @return Response
	 * @throws IOException
	 */
	public void topVideo() throws IOException {
		printData(writer, "Top Videos", analytics.reports().query("channel==" + channelID, "2011-01-01", "2014-12-05", "views,subscribersGained,subscribersLost").setDimensions("video").setSort("-views").setMaxResults(10).execute()); 	
	}
	
	private void printData(PrintStream writer, String title, ResultTable results) {
		writer.println("Report: " + title);
		
		if(results.getRows() == null || results.getRows().isEmpty()) {
			writer.println("No results found.");
		} else {
			/* Print column headers */
			for(ColumnHeaders header : results.getColumnHeaders()) {
				writer.printf("%30s", header.getName());
			}
			writer.println(); 
			
			/* Print content */
			for(List<Object> row : results.getRows()) {
				for(int columnNum = 0; columnNum < results.getColumnHeaders().size(); columnNum++) {
					ColumnHeaders header = results.getColumnHeaders().get(columnNum);
					Object column = row.get(columnNum);
					
					if("INTEGER".equals(header.getUnknownKeys().get("dataType"))) {
						long longResult = ((BigDecimal) column).longValue(); 
						writer.printf("%30d", longResult);
					} else if("FLOAT".equals(header.getUnknownKeys().get("dataType"))) {
						writer.printf("%30f", column);
					} else if("STRING".equals(header.getUnknownKeys().get("dataType"))) {
						writer.printf("%30s", column);
					} else {
						writer.printf("%30s", column); 
					}
				}
				writer.println();
			}
			writer.println(); 
		}
	}
}
