package gDrive;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

/**
 * A simple program that interacts with Google Drive
 * 
 * References: 
 * https://developers.google.com/drive/web/quickstart/quickstart-java
 * 
 * http://stackoverflow.com/questions/5226212/how-to-open-the-default-webbrowser-using-java
 * 
 * @author Emil Tan
 */
public class GettingStarted {

	/** To make Drive API requests */
	private Drive service; 
	
	/** 
	 * Constructor - Acquire authorisation from user using OAuth 2.0
	 * 
	 * @param clientID OAuth Client ID
	 * @param clientSecret OAuth Client Secret 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public GettingStarted(String clientID, String clientSecret) throws IOException, URISyntaxException {
		HttpTransport httpTransport = new NetHttpTransport(); 
		JsonFactory jsonFactory = new JacksonFactory(); 
		
		String redirectURL = "urn:ietf:wg:oauth:2.0:oob"; 
		
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientID, clientSecret, Arrays.asList(DriveScopes.DRIVE)).setAccessType("online").setApprovalPrompt("auto").build(); 
	
		String authorisationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectURL).build(); 
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
		
		GoogleTokenResponse response = flow.newTokenRequest(authorisationCode).setRedirectUri(redirectURL).execute(); 
		
		GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);
		
		service = new Drive.Builder(httpTransport, jsonFactory, credential).build(); 
	}
	
	/**
	 * Take a text/plain file and copy its content to user's Google Drive.
	 * 
	 * @param fileName Source file to copy from
	 * @param fileTitle Destination File name to copy into
	 * @param fileDescription Description of the file
	 * @throws IOException
	 */
	public void copyTextFile(String fileName, String fileTitle, String fileDescription) throws IOException {
		File body = new File(); 
		body.setTitle(fileTitle); 
		body.setDescription(fileDescription); 
		body.setMimeType("text/plain");
		
		java.io.File fileContent = new java.io.File(fileName); 
		FileContent mediaContent = new FileContent("text/plain", fileContent);
		
		File file = service.files().insert(body, mediaContent).execute(); 
		
		System.out.println("File created. File ID: " + file.getId());
	}
}
