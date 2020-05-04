package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.uuid.Generators;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;


public class LambdaFunctionHandler implements RequestHandler<Object, String> {
        static Context context;
        JSONObject error=new JSONObject("{\"error\":\"Invalied request\"}");
	  static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	    static DynamoDB dynamoDB = new DynamoDB(client);
	    JSONObject json;
	    static long count;
	    static boolean flag = true;
	    
    @Override
    public String handleRequest(Object input, Context cxt) {
     context=cxt;
     context.getLogger().log("Input"+input);
     try {
    	 json=new JSONObject(""+input);
    	 return getData(json).toString();
    	 
     }catch(JSONException e) {
    	 
     }
	return "Invalied Request";
   
    }


	private Object getData(JSONObject input) {
		if(input==null) {
			return error;
		}else {
			try {
				switch(input.getString("name")) {
				case "getQuiz": return getQuiz(1);
				case "getUser":return getUser(input.getString("userName"));
				case "getHistory":return getHistory(input.getString("userId"));
				case "setScore":return setScore(input.getString("userId"),input.getNumber("score"),input.getNumber("clientId"));
				default:return error;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		return error;
	}
	private boolean setScore(String userId, Number score, Number clientId){
		Table table = dynamoDB.getTable("user");
		try {
			Item item=table.getItem("userId",userId);
			
			  Date today = new Date();
			 DateFormat df = new SimpleDateFormat("dd-MM-yy HH:mm:SS z");
		        df.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
				String IST = df.format(today);
			 
			 
			 JSONArray scores = new JSONArray();
         	scores.put(new JSONObject().put("clientId",clientId)
         	                           .put("score",score)
         	                           .put("datetimeString",IST));

            	UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                        .withPrimaryKey("userId", userId)
                        .withNameMap(new NameMap().with("#P", "scores"))
                        .withValueMap(new ValueMap()
                                .withJSON(":val", scores.toString()) 
                                .withList(":empty_list", new ArrayList<>()))
                        .withUpdateExpression("SET #P = list_append(if_not_exists(#P, :empty_list), :val)");
            	UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            	return true;
 
		}catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}


	private Object getHistory(String userId) {
		Table table = dynamoDB.getTable("user");
    	try {

    		Item item = table.getItem("userId",userId,"scores",null);
    		return new JSONObject("" + item.toJSONPretty());
    	}catch(Exception e) {
    		context.getLogger().log("GetItem Failed");
    		context.getLogger().log(e.getMessage());
    	}
    	return error;
	}


	private Object getQuiz(int quizId) {
		Table table=dynamoDB.getTable("QuizData");
		try {
			Item item=table.getItem("quizId",quizId);
			
			context.getLogger().log(item.toJSONPretty());
			return new JSONObject(""+item.toJSONPretty());
		}catch (JSONException e) {
			context.getLogger().log("getItem failed");
			context.getLogger().log(e.getMessage());
		}		
		return error;	
	}	
   
	
	
	private Object getUser(String username) {
    	Table table = dynamoDB.getTable("user");
		Item item=getItemHelper(username, table);
	  if(item==null){
		      String uid=createuser(username);
	           if(uid!=null) {
	        	   try {
					Item i=table.getItem("userId",uid,"userId",null);
					context.getLogger().log(i.toJSONPretty());
					return new JSONObject(""+i.toJSONPretty());
				} catch (JSONException e) {
					context.getLogger().log("can't get the"+uid);
					e.printStackTrace();
				}
	           }
	  }else {
		  try {
			  	String uid=(String) item.get("userId");
			  	Item i=table.getItem("userId",uid,"userId",null);
				context.getLogger().log(i.toJSONPretty());
				return new JSONObject(""+i.toJSONPretty());
			}
		     catch (JSONException e) {
				context.getLogger().log("can't get the player");
				e.printStackTrace();
			}
	  }
	return error; 
    }

	
     private String createuser(String userName) {
    	 Table table = dynamoDB.getTable("user");
	     try{ 
	    	 UUID uuid1 = Generators.timeBasedGenerator().generate();
	    	 String userId=uuid1.toString();
	    	 
	    	 Item item = new Item()
	    			    .withPrimaryKey("userId", userId)
	    			    .withString("userName",userName);
	    			    
	    	             
	    	 PutItemOutcome outcome = table.putItem(item);
	    	 return userId;
	    	 }catch(Exception e) {
	    		 context.getLogger().log("Error in creating item");
			e.printStackTrace();
		}
    	 return null;
        }
     
     
     //this method just searches for a username and return the Item of the user 
     private Item getItemHelper(String username ,Table table) {
    	 Index index = table.getIndex("userName-index");
    	 QuerySpec spec = new QuerySpec()
    			    .withKeyConditionExpression("#nm = :username")
    			    .withNameMap(new NameMap()
    			        .with("#nm", "userName"))
    			    .withValueMap(new ValueMap()
    			        .withString(":username",username));
    	 ItemCollection<QueryOutcome> items = index.query(spec);
    	 for(Item i:items) {
    		 return i;
    	 }
    	 return null;
	 }

}
//"{\"name\":\"getuser\",\"userName\":\"c\"}"
//"{\"name\":\"getHistory\",\"userId\":\"79eac60e-8df3-11ea-808a-f7be9269ee12\"}"


//"{\"name\":\"getQuiz\",\"quizId\":1}"
//"{\"name\":\"setScore\",\"userId\":\"79eac60e-8df3-11ea-808a-f7be9269ee12\",\"score\":\"5\",\"clientId\":\"2\"}"

//  "{\"name\":\"${input.params('name')}\",\"playerid\":\"${input.params('pid')\"}}"