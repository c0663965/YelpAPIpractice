package YelpData;

//https://www.yelp.com/developers/documentation/v2/search_api   : 

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Code sample for accessing the Yelp API V2.
 * 
 * This program demonstrates the capability of the Yelp API version 2.0 by using the Search API to
 * query for businesses by a search term and location, and the Business API to query additional
 * information about the top result from the search query.
 * 
 * <p>
 * See <a href="http://www.yelp.com/developers/documentation">Yelp Documentation</a> for more info.
 * 
 */
public class YelpAPI {

  private static final String API_HOST = "api.yelp.com";
  private static final String DEFAULT_TERM = "restaurant";
  private static final String DEFAULT_LOCATION = "Sarnia";
  private static final int SEARCH_LIMIT =20;
  private static final String SEARCH_PATH = "/v2/search";
  private static final String BUSINESS_PATH = "/v2/business";
  //private static final int OFFSET=1;

  /*
   * Update OAuth credentials below from the Yelp Developers API site:
   * http://www.yelp.com/developers/getting_started/api_access
   */
  private static final String CONSUMER_KEY = "a0IAK0pUpQ63MplFyS66Ag";
  private static final String CONSUMER_SECRET = "u18VhfQr9ItEgJ4R1hcT3y35Eto";
  private static final String TOKEN = "TeW6HS2YsAVHLs3pC_jEWG5XvgX0TkSB";
  private static final String TOKEN_SECRET = "baYozpg5tw5FoA4WWYQywzG8s54";
  private static List<Restaurant> searchedInfo= new ArrayList();
  private static final String folderPath = "F:/JavaApplication6/images/";

  

  OAuthService service;
  Token accessToken;

  /**
   * Setup the Yelp API OAuth credentials.
   * 
   * @param consumerKey Consumer key
   * @param consumerSecret Consumer secret
   * @param token Token
   * @param tokenSecret Token secret
   */
  public YelpAPI(String consumerKey, String consumerSecret, String token, String tokenSecret) {
    this.service =
        new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey)
            .apiSecret(consumerSecret).build();
    this.accessToken = new Token(token, tokenSecret);
  }

  /**
   * Creates and sends a request to the Search API by term and location.
   * <p>
   * See <a href="http://www.yelp.com/developers/documentation/v2/search_api">Yelp Search API V2</a>
   * for more info.
   * 
   * @param term <tt>String</tt> of the search term to be queried
   * @param location <tt>String</tt> of the location
   * @return <tt>String</tt> JSON Response
   */
  public String searchForBusinessesByLocation(String term, String location, int OFFSET) {
    OAuthRequest request = createOAuthRequest(SEARCH_PATH);
    request.addQuerystringParameter("term", term);
    request.addQuerystringParameter("location", location);
    request.addQuerystringParameter("limit", String.valueOf(SEARCH_LIMIT));
    request.addQuerystringParameter("offset",String.valueOf(OFFSET));
    request.addQuerystringParameter("sort", "0");
    
    return sendRequestAndGetResponse(request);
  }

  /**
   * Creates and sends a request to the Business API by business ID.
   * <p>
   * See <a href="http://www.yelp.com/developers/documentation/v2/business">Yelp Business API V2</a>
   * for more info.
   * 
   * @param businessID <tt>String</tt> business ID of the requested business
   * @return <tt>String</tt> JSON Response
   */
  public String searchByBusinessId(String businessID) {
    OAuthRequest request = createOAuthRequest(BUSINESS_PATH + "/" + businessID);
    return sendRequestAndGetResponse(request);
  }

  /**
   * Creates and returns an {@link OAuthRequest} based on the API endpoint specified.
   * 
   * @param path API endpoint to be queried
   * @return <tt>OAuthRequest</tt>
   */
  private OAuthRequest createOAuthRequest(String path) {
    OAuthRequest request = new OAuthRequest(Verb.GET, "https://" + API_HOST + path);
    return request;
  }

  /**
   * Sends an {@link OAuthRequest} and returns the {@link Response} body.
   * 
   * @param request {@link OAuthRequest} corresponding to the API request
   * @return <tt>String</tt> body of API response
   */
  private String sendRequestAndGetResponse(OAuthRequest request) {
    System.out.println("Querying " + request.getCompleteUrl() + " ...");
    this.service.signRequest(this.accessToken, request);
    Response response = request.send();
    return response.getBody();
  }

  /**
   * Queries the Search API based on the command line arguments and takes the first result to query
   * the Business API.
   * 
   * @param yelpApi <tt>YelpAPI</tt> service instance
   * @param yelpApiCli <tt>YelpAPICLI</tt> command line arguments
   */
  private static void queryAPI(YelpAPI yelpApi, YelpAPICLI yelpApiCli, int totalPages) throws SQLException {
    
    for (int offset=0; offset<totalPages ; offset++)  
    {
        String searchResponseJSON =
            yelpApi.searchForBusinessesByLocation(yelpApiCli.term, yelpApiCli.location, offset);

        JSONParser parser = new JSONParser();
        JSONObject response = null;

        try {
          response = (JSONObject) parser.parse(searchResponseJSON);
        } catch (ParseException e) {
          System.out.println("Error: could not parse JSON response:");
          System.out.println(searchResponseJSON);
          System.exit(1);
        }

        JSONArray businesses = (JSONArray) response.get("businesses");
                
        clearFolder();
        
        for (int i=0; i<businesses.size();i++) 
        {
            JSONObject fb = (JSONObject) businesses.get(i);
            String NAME = fb.get("name").toString();  
            
            JSONArray category = (JSONArray)fb.get("categories");
            JSONArray menu = (JSONArray)category.get(0);
            String MENU = menu.get(0).toString();
           
            JSONObject location =(JSONObject)fb.get("location");
            JSONArray address= (JSONArray)location.get("display_address");
            
            String PHONE = fb.get("display_phone").toString();
            String ADDRESS="";
            String MOBILE_URL = fb.get("mobile_url").toString();
            //String URL = fb.get("url").toString();
            String RATING = fb.get("rating").toString();
            String RATING_IMAGE_URL =fb.get("rating_img_url").toString();
            String SNIPPET_TEXT=fb.get("snippet_text").toString().replace("\n","");
           
            for (Object str : address) {
                ADDRESS += str.toString() + ", ";
            }
            ADDRESS=ADDRESS.substring(0, ADDRESS.length()-2);
            
            String POSTAL_CODE=location.get("postal_code").toString();  
            String COUNTRY_CODE=location.get("country_code").toString();
            JSONObject position = (JSONObject)location.get("coordinate");
            String LATITUDE = position.get("latitude").toString();
            String LONGITUDE = position.get("longitude").toString();
            
            Restaurant bistro = new Restaurant();
            
            bistro.setNAME(NAME);
            bistro.setMENU(MENU);
            bistro.setPHONE(PHONE);
            bistro.setADDRESS(ADDRESS);
            bistro.setPOSTAL_CODE(POSTAL_CODE);
            bistro.setCOUNTRY_CODE(COUNTRY_CODE);
            bistro.setLATITUDE(Double.parseDouble(LATITUDE));
            bistro.setLONGITUDE(Double.parseDouble(LONGITUDE));
            bistro.setMOBILE_URL(MOBILE_URL);
            
            try {
                Document doc = Jsoup.connect(MOBILE_URL).get();
                Elements img = doc.getElementsByClass("photo-box-img").eq(0);
                
                
//                for (Element el : img)
//                { 
//                    String src = el.absUrl("src"); 
//                    System.out.println("Image Found!"); 
//                    System.out.println("src attribute is : "+src); 
                
                String src=img.attr("src");
                String name=(i+1)+".jpg";
                getImages(src,name); 
                //} 

            } catch (IOException ex) {
                Logger.getLogger(YelpAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            bistro.setRATING(RATING);
            bistro.setRATING_IMAGE_URL(RATING_IMAGE_URL);
            bistro.setSNIPPET_TEXT(SNIPPET_TEXT);
            
            searchedInfo.add(bistro);
        }  
    }
}
  
  private static void getImages(String src, String name) throws IOException {

//      http://examples.javacodegeeks.com/enterprise-java/html/download-images-from-a-website-using-jsoup/
//      Exctract the name of the image from the src attribute
//        
//        int indexname = src.lastIndexOf("/"); //The index number
//        
//        if (indexname == src.length()) 
//        {
//            src = src.substring(1, indexname);
//        }
//        
//        
//        indexname = src.lastIndexOf("/");
//        String name = src.substring(indexname, src.length());
//        System.out.println(name);
//        Open a URL Stream
        
        URL url = new URL(src);
        
        InputStream in = url.openStream();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(folderPath+name));
        
        for (int b; (b = in.read()) != -1;) 
            out.write(b);
        
        out.close();
        
        in.close();
    }

    private static void clearFolder(){
        
      File file = new File(folderPath);      

      String[] myFiles;    

      if(file.isDirectory())
      {
          myFiles = file.list();

          for (int i=0; i<myFiles.length; i++) 
          {
              File myFile = new File(file, myFiles[i]); 
              myFile.delete();
          }
      }
    }
  
  /**
   * Command-line interface for the sample Yelp API runner.
   */
  private static class YelpAPICLI {
    @Parameter(names = {"-q", "--term"}, description = "Search Query Term")
    public String term = DEFAULT_TERM;

    @Parameter(names = {"-l", "--location"}, description = "Location to be Queried")
    public String location = DEFAULT_LOCATION;
  }

  /**
   * Main entry for sample Yelp API requests.
   * <p>
   * After entering your OAuth credentials, execute <tt><b>run.sh</b></tt> to run this example.
     * @param args
     * @throws java.sql.SQLException
   */
  public static void main(String[] args) throws SQLException {
 
    YelpAPICLI yelpApiCli = new YelpAPICLI();
    //new JCommander(yelpApiCli, args);

    YelpAPI yelpApi = new YelpAPI(CONSUMER_KEY, CONSUMER_SECRET, TOKEN, TOKEN_SECRET);
    
    int numOfPages=1;// 20 restaurants per a page. 
    queryAPI(yelpApi, yelpApiCli, numOfPages);
    
    DBconnection DB=new DBconnection();
    DB.createTable();
    DB.insertData(searchedInfo);
  }
}