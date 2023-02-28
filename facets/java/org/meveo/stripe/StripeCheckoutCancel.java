package org.meveo.stripe;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.meveo.service.script.Script;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.StrCheckoutInfo;


import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;
import org.meveo.email.service.EmailService;

import org.meveo.model.customEntities.Credential;
import org.meveo.credentials.CredentialHelperService;

public class StripeCheckoutCancel extends EndpointScript {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeCheckoutCancel.class);
    private static final String SUCCESS = "success";    
      
    @Inject
	private RepositoryService repositoryService;
  
    @Inject
    private CrossStorageApi crossStorageApi;
  
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
        boolean sendEmail = false;
        String sessionId = "";
      
        // retrieve apiKey from credential
        /*if(CheckoutSessionScript.STRIPE_CHECKOUT_API_KEY == null || CheckoutSessionScript.STRIPE_CHECKOUT_API_KEY.trim().length() == 0 ){
          Credential credential = CredentialHelperService.getCredential(CheckoutSessionScript.STRIPE_DOMAIN, crossStorageApi, repositoryService.findDefaultRepository());
          CheckoutSessionScript.STRIPE_CHECKOUT_API_KEY = (credential == null ? "": credential.getApiKey());
          Stripe.apiKey = CheckoutSessionScript.STRIPE_CHECKOUT_API_KEY;
          if(credential == null)
            throw new BusinessException("no api keys found against stripe.com domain");
        }*/
      
        if (parameters.containsKey("sessionId") && parameters.get("sessionId") != null) {
			sessionId = parameters.get("sessionId").toString();
		} else {
            Map object = (HashMap)((HashMap)parameters.get("data")).get("object");
            sessionId = object.get("id").toString(); 
            sendEmail = true;
            if(sessionId == null ){
              	endpointResponse.setStatus(400);
				endpointResponse.setErrorMessage("missing sessionId");
            	return;
            }
		}
        
        if(sessionId.length() > 66){
            endpointResponse.setStatus(400);
			endpointResponse.setErrorMessage("sessionId length exceeds 66 characters");
            return;
        }
        
		Log.info("===============================================================");
        Log.info("sessionId="+sessionId);
        
        
        Session session = null;        
        String checkoutInfoId = null;
        String customerEmail = null;
        try{
            session = Session.retrieve( sessionId.trim() );
            checkoutInfoId = session.getMetadata().get("checkoutInfoId");
            if(session.getCustomerDetails() != null){
                customerEmail = session.getCustomerDetails().getEmail();
            }
        }catch(StripeException ex)  {
            Log.error(ex.getMessage());
            throw new BusinessException( ex);
        }
      
        Log.info("checkoutInfoId="+checkoutInfoId);
        Log.info("customerEmail="+(customerEmail == null ? "null" : customerEmail));
        Log.info("Payment Status ="+session.getPaymentStatus());
        Log.info("Status ="+session.getStatus());
        Log.info("===============================================================");
      
        if(checkoutInfoId != null && checkoutInfoId.length() > 0){
          	Repository defaultRepo = repositoryService.findDefaultRepository();
            try {
                StrCheckoutInfo checkoutInfo = crossStorageApi.find(defaultRepo, checkoutInfoId, StrCheckoutInfo.class);
                checkoutInfo.setResponseCode("400");
                checkoutInfo.setResponse(session.toString());
                crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);            
                Log.info("checkoutInfo instance {} updated", checkoutInfoId);
            } catch (Exception ex) {
                throw new BusinessException(ex);
            }
        }else{
            Log.error("Missing Data - No checkout Info Id is found");
        }
        
        if(customerEmail != null && sendEmail){            		
            this.sendFailureEmail(customerEmail);
        }
	}
  
    private void sendFailureEmail(String emailAddressTo) throws BusinessException{
        String result = null;
        
        Map<String, Object> mapping = new HashMap<>();
      	mapping.put("emailType", "ONBOARDING_FAIL_TPK");
      	mapping.put("emailAddressTo", emailAddressTo);
      	mapping.put("mapping", new HashMap<String, String>());
      
        Script emailService = new EmailService();
        if(emailService == null){
            Log.info("Email Srevice is not initialized");
            result = "Failed";
        }else{
            emailService.execute(mapping);
            result = SUCCESS;
        }
        Log.info("result: {}", result);
    }


}