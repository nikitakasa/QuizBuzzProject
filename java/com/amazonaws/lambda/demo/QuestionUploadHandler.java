package com.amazonaws.lambda.demo;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class QuestionUploadHandler implements RequestHandler<Object, String> {
	  static Context context;
	 JSONObject error=new JSONObject("{\"error\":\"Invalied request\"}");
	  static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	    static DynamoDB dynamoDB = new DynamoDB(client);
	    JSONObject json;	    

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
				
				default:return error;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		return error;
	}

}
