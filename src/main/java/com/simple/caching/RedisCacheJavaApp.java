package com.simple.caching;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisCacheJavaApp {
	
	//Initialize Jedis pool
	static JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
	
	/*
	 * The App repeatedly asks user for a photo id. If the photo is available in cache(redis), it is served from cache
	 * and the latency is very low. If the photo is not available in cache, it is fetched from server and 
	 * stored in cache as well for future requests.
	 */
	public static void main(String [] args) throws Exception{

		System.out.println("Enter the id of photo be fetched. Press X to exit");
		Scanner in = new Scanner(System.in);
		String id = in.nextLine();
		
		do{
			try {
				Integer.parseInt(id);
			}catch(NumberFormatException e) {
				System.out.println("Plesae Enter a valid Photo Id");
				id = in.nextLine();
				continue;
			}

			long startTime = System.currentTimeMillis();
			
			//Check if photo is available in cache.
			String photo = getValueFromCache(id);
			if(photo != null){
				System.out.println("Photo "+ id+" found in cache :" + photo);
			}else{
				//Fetch photo from service if not available in cache and store it in cache.
				photo = getPhoto(id);
				setValueInCache(id,photo);
				System.out.println("Photo "+ id+" fetched from server and stored in cache :" + photo);
			}
			long endTime = System.currentTimeMillis();
			System.out.println("Execution time in milliseconds : " + (endTime-startTime));

			id = in.nextLine();

		} while(!id.equals("X"));
		in.close();

	}
	
	//Fetch photo for given id from jsonplaceholder service
	private static String getPhoto(String id) throws Exception{
		
		String httpsURL = "http://jsonplaceholder.typicode.com/photos/"+id;
        URL myUrl = new URL(httpsURL);
        HttpURLConnection conn = (HttpURLConnection)myUrl.openConnection();
        InputStream is = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
 
        StringBuffer res = new StringBuffer();
        String inputLine;
        while ((inputLine = br.readLine()) != null) {
        	res.append(inputLine);
        }
        br.close();
        return res.toString();
		
	}
	
	//Set a key in cache
	private static void setValueInCache(String key, String val){
		Jedis jedis = null;
		try {
		  jedis = pool.getResource();
		  jedis.set(key, val);
		} finally {
		  if (jedis != null) {
		    jedis.close();
		  }
		}
	}

	//Get a key from cache
	private static String getValueFromCache(String key){
		Jedis jedis = null;
		String val = null;
		try {
		  jedis = pool.getResource();
		  val = jedis.get(key);
		} finally {
		  if (jedis != null) {
		    jedis.close();
		  }
		}
		return val;
	}

}
