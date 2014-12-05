package gCalendar;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

/**
 * A simple application that uses Google Calendar
 * 
 * References: 
 * Getting authorisation: https://developers.google.com/google-apps/calendar/instantiate
 * https://developers.google.com/google-apps/calendar/v3/reference/events/insert
 * 
 * http://stackoverflow.com/questions/5226212/how-to-open-the-default-webbrowser-using-java
 * 
 * @author Emil Tan
 */
public class GettingStarted {
	private Calendar service; 	
	private String applicationName; 
	private String clientID;
	private String clientSecret; 

	/** 
	 * Constructor - Acquire authorisation from user using OAuth 2.0
	 *  
	 * @param applicationName
	 * @param clientID OAuth Client ID
	 * @param clientSecret OAuth Client Secret 
	 * @throws URISyntaxException 
	 */
	public GettingStarted(String applicationName, String clientID, String clientSecret) throws IOException, GeneralSecurityException, URISyntaxException {
		this.applicationName = applicationName; 
		this.clientID = clientID;
		this.clientSecret = clientSecret;

		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport(); 
		JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance(); 

		String redirectUrl = "urn:ietf:wg:oauth:2.0:oob"; 
		String scope = "https://www.googleapis.com/auth/calendar";

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow(httpTransport, jsonFactory, clientID, clientSecret, Collections.singleton(scope));

		String authorisationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUrl).build();
		String authorisationCode = ""; 
		if(Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(new URI(authorisationUrl));
		} else {
			System.out.println("Go to the following link in your browser: ");
			System.out.println(authorisationUrl);
		}

		// Read the authorisation code from the standard input stream 
		BufferedReader bufReader = new BufferedReader(new InputStreamReader(System.in)); 
		System.out.println("What is the authorisation code? ");
		authorisationCode = bufReader.readLine(); 


		GoogleTokenResponse response = flow.newTokenRequest(authorisationCode).setRedirectUri(redirectUrl).execute();

		Credential credential = new GoogleCredential.Builder().setTransport(httpTransport).setJsonFactory(jsonFactory).setClientSecrets(clientID, clientSecret).build().setFromTokenResponse(response);

		service = new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build();
	}

	/**
	 * Insert events into Google Calendar 
	 * 
	 * @param eventSummary Description of the event to be added 
	 * @param eventLocation Where the event will be held at
	 */
	public void insert(String eventSummary, String eventLocation) throws IOException {
		Event event = new Event(); 

		event.setSummary(eventSummary); 
		event.setLocation(eventLocation); 

		Date startDate = new Date(); 
		Date endDate = new Date(startDate.getTime() + 3600000);

		DateTime start = new DateTime(startDate);
		event.setStart(new EventDateTime().setDateTime(start)); 
		DateTime end = new DateTime(endDate);
		event.setEnd(new EventDateTime().setDateTime(end)); 

		Event createdEvent = service.events().insert("primary", event).execute();

		System.out.println(createdEvent.getId());
	}
}
