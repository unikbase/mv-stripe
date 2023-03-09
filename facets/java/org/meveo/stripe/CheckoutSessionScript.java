package org.meveo.stripe;

import javax.inject.Inject;
import java.util.Map;
import java.time.Instant;
import java.util.HashMap;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.StrCheckoutInfo;
import org.meveo.model.customEntities.Credential;
import org.meveo.credentials.CredentialHelperService;

import org.meveo.api.rest.technicalservice.EndpointScript;

public class CheckoutSessionScript extends EndpointScript {

	private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);

	private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
	private static final Logger Log = LoggerFactory.getLogger(CheckoutSessionScript.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
  
    public static final String STRIPE_DOMAIN = "stripe.com";    
    public static final String ENV_ACCOUNT_TYPE_DEV = "ENV_ACCOUNT_TYPE_DEV";
    public static final String ENV_ACCOUNT_TYPE_PREPROD = "ENV_ACCOUNT_TYPE_PREPROD";
    public static final String ENV_ACCOUNT_TYPE_LIVE = "ENV_ACCOUNT_TYPE_LIVE";
    public static final String DEV_SUCCESS = "DEV_SUCCESS";
    public static final String DEV_FAILURE = "DEV_FAILURE";
    public static final String LIVE_SUCCESS = "LIVE_SUCCESS";
    public static final String LIVE_FAILURE = "LIVE_FAILURE";
    public static final String PREPROD_SUCCESS = "PREPROD_SUCCESS";
    public static final String PREPROD_FAILURE = "PREPROD_FAILURE";
    public static final String DEV_STRIPE_ACCOUNT_EMAIL = "farhan.munir@qavitech.com";
    public static final String PREPROD_STRIPE_ACCOUNT_EMAIL = "farhan.munir@gmail.com";
    public static final String LIVE_STRIPE_ACCOUNT_EMAIL = "smichea@manaty.net";

    public static Map<String, Map<String, String>> ALL_PRICE_MAPS;    
    private static Map<String, String> SUCCESS_FAILURE_URLS;
  
    public static Map<String, String> PRICE_MAP;
    public static final String SUCCESS_URL;
    public static final String CANCEL_URL;
    public static String STRIPE_CHECKOUT_API_KEY;
  
    static{
      
        ALL_PRICE_MAPS = CheckoutSessionScript.getInitializedMap();
        SUCCESS_FAILURE_URLS = CheckoutSessionScript.initializeURLs();
      
        // Need to change parameters in all three lines according to the environement for now.
        PRICE_MAP = ALL_PRICE_MAPS.get(ENV_ACCOUNT_TYPE_LIVE);
        SUCCESS_URL = SUCCESS_FAILURE_URLS.get(LIVE_SUCCESS);
        CANCEL_URL = SUCCESS_FAILURE_URLS.get(LIVE_FAILURE);
        
    }
  
   
	private String responseUrl="";

	public String getResponseUrl() {
		return responseUrl;
	}
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		Log.info("received {}", parameters);
        String priceId = "2";
		StrCheckoutInfo checkoutInfo = new StrCheckoutInfo();
		checkoutInfo.setCreationDate(Instant.now());

      
        Map<String, String> inputInfo = new HashMap<>();
		if (parameters.containsKey("tpk_id")) {
			inputInfo.put("tpk_id", parameters.get("tpk_id").toString());
		}
        if (parameters.containsKey("price_id")) {
			priceId = parameters.get("price_id").toString();
            if(PRICE_MAP.get(priceId) == null){
                throw new BusinessException("No Price is defined against price_id provided");
            }else if("0".equals(PRICE_MAP.get(priceId))){
                if(parameters.get("email") == null){
                    endpointResponse.setStatus(400);
			        endpointResponse.setErrorMessage("missing email, if provided we can send email");
                }else{
                    responseUrl = this.endpointRequest.getRequestURL().toString().substring(0,this.endpointRequest.getRequestURL().toString().indexOf("/rest/"))+"/rest/stripeNoPaymentCheckoutSuccess?customerEmail="
                      					+parameters.get("email").toString()+"&tpkId="+inputInfo.get("tpk_id").toString();
                }                
                return;
            }
		}
		
		if (parameters.containsKey("value")) {
			inputInfo.put("value", parameters.get("value").toString());
		}
		if (parameters.containsKey("token")) {
			inputInfo.put("token", parameters.get("token").toString());
		}
        
		ObjectMapper objectMapper = new ObjectMapper();
		String json = null;
		try {
			json = objectMapper.writeValueAsString(inputInfo);
		} catch (JsonProcessingException e) {
			throw new BusinessException(e);
		}
		checkoutInfo.setInputInfo(json);

		Repository defaultRepo = repositoryService.findDefaultRepository();
        String uuid = null;
		try {
			uuid = crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);
			Log.info("checkoutInfo instance {} created", uuid);
		} catch (Exception ex) {
			throw new BusinessException(ex);
		}

		// retrieve apiKey from credential
		Credential credential = CredentialHelperService.getCredential(STRIPE_DOMAIN, crossStorageApi, defaultRepo);
        STRIPE_CHECKOUT_API_KEY = (credential == null ? "": credential.getApiKey());
        Stripe.apiKey = STRIPE_CHECKOUT_API_KEY;
        Log.info("stripe api key = "+STRIPE_CHECKOUT_API_KEY);
        Log.info("price tag = "+PRICE_MAP.get(priceId));
        try {
			SessionCreateParams params = SessionCreateParams.builder()
					.setMode(SessionCreateParams.Mode.PAYMENT)
					.setSuccessUrl(SUCCESS_URL)
					.setCancelUrl(CANCEL_URL)
					.setAutomaticTax(
							SessionCreateParams.AutomaticTax.builder()
									.setEnabled(true)
									.build())
					.addLineItem(
							SessionCreateParams.LineItem.builder()
									.setQuantity(1L)
									.setPrice(PRICE_MAP.get(priceId))
									.build())
                    .putMetadata("checkoutInfoId",uuid) 
                    //.putMetadata("customerEmail",checkoutInfo.getEmail())
                    .putMetadata("price",PRICE_MAP.get(priceId))
                    .putMetadata("tpkId",inputInfo.get("tpk_id"))
                    .putMetadata("inputPriceId",priceId)
					.build();
			Session session = Session.create(params);
          
			Log.info("session {}", session);
			responseUrl = session.getUrl();
			Log.info("responseUrl {}", responseUrl);
		} catch (StripeException ex) {
			Log.error("Stripe error", ex);
		}
	}
  
    static Map<String, Map<String, String>> getInitializedMap(){
        Map<String, Map<String, String>> envPriceMap = new HashMap<>();
        
        Map<String, String> priceMap = new HashMap();
        priceMap.put("0", "0");
        priceMap.put("1", "price_1MWOHoJQmmmLLXjqRUnYQ69J");
        priceMap.put("2", "price_1MU8ueJQmmmLLXjqGx5Qjblb");
        priceMap.put("3", "price_1MWOILJQmmmLLXjqLjuLjV9y");
        priceMap.put("4", "price_1MWOHoJQmmmLLXjqRUnYQ69J");
        priceMap.put("5", "price_1MWOIgJQmmmLLXjqT9epLq7d");
        envPriceMap.put(ENV_ACCOUNT_TYPE_DEV, priceMap);
      
        priceMap = new HashMap();
        priceMap.put("0", "0");
        priceMap.put("1", "price_1MWQMaLksAz2eKAnTa6RbDHz");
        priceMap.put("2", "price_1MVw9gLksAz2eKAnfEpX4oKv");
        priceMap.put("3", "price_1MWQMpLksAz2eKAnWZf30tXY");
        priceMap.put("4", "price_1MWQMaLksAz2eKAnTa6RbDHz");
        priceMap.put("5", "price_1MWQN0LksAz2eKAnfes7S1vf");
        envPriceMap.put(ENV_ACCOUNT_TYPE_PREPROD, priceMap);
      
        priceMap = new HashMap();
        priceMap.put("0", "0");
        priceMap.put("1", "price_1MWFI4F8O6FLWQWJHPlngM0j");
        priceMap.put("2", "price_1MXMuPF8O6FLWQWJq9DBrPbM");
        priceMap.put("3", "price_1MWFIPF8O6FLWQWJ6yG5an6E");
        priceMap.put("4", "price_1MWFI4F8O6FLWQWJHPlngM0j");
        priceMap.put("5", "price_1MWFIjF8O6FLWQWJFmvNU0b9");
        envPriceMap.put(ENV_ACCOUNT_TYPE_LIVE, priceMap);
        
        return envPriceMap;
    }
  
    static Map<String, String> initializeURLs(){
      Map<String, String> urls = new HashMap();
      urls.put("DEV_SUCCESS", "https://unikbase-infra.github.io/env-onboarding-dev/#/success");
      urls.put("DEV_FAILURE", "https://unikbase-infra.github.io/env-onboarding-dev/#/cancel");
      urls.put("PREPROD_SUCCESS", "https://unikbase-infra.github.io/env-onboarding-dev/#/success");
      urls.put("PREPROD_FAILURE", "https://unikbase-infra.github.io/env-onboarding-dev/#/cancel");
      urls.put("LIVE_SUCCESS", "http://onboarding.unikbase.com/#/success");
      urls.put("LIVE_FAILURE", "http://onboarding.unikbase.com/#/cancel");
      return urls;
    }

}