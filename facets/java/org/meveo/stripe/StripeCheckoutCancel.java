package org.meveo.stripe;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;


import org.meveo.service.script.Script;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.StrCheckoutInfo;
import org.meveo.commons.utils.MailerSessionFactory;


import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;

public class StripeCheckoutCancel extends EndpointScript {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeCheckoutCancel.class);
    private static final String SUCCESS = "success";    
    private String sessionId;
  
    @Inject
	private RepositoryService repositoryService;
  
    @Inject
    private CrossStorageApi crossStorageApi;
  
    @Inject
    private MailerSessionFactory mailerSessionFactory;
  
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
        boolean sendEmail = false;
        //this.sessionId = "cs_test_a1BENXl7ATXp0QvAtyjM1DQRsf4OM1Rv8QqAFKmk20xMsh9eBnTHSqfVuJ";
        Stripe.apiKey = "sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3";
      
		
        if(this.sessionId == null || this.sessionId.trim().length() == 0 ){
            Log.info("Called from stripe.com for payment failure");
            sendEmail = true;
            Map object = (HashMap)((HashMap)parameters.get("data")).get("object");
            this.sessionId = object.get("id").toString();            
        }
        Log.info("===============================================================");
      	Log.info("this.sessionId="+this.sessionId);
        Log.info("===============================================================");
      
        Session session = null;
        Map<String, String> metadata = null;
        try{
            session = Session.retrieve( this.sessionId.trim() );
            metadata = session.getMetadata();
        }catch(StripeException ex)  {
            Log.error(ex.getMessage());
            throw new BusinessException( ex);
        }
      
        String checkoutInfoId = metadata.get("checkoutInfoId");
        String customerEmail = metadata.get("customerEmail");
        Log.info("checkoutInfoId="+metadata.get("checkoutInfoId"));
        Log.info("customerEmail="+metadata.get("customerEmail"));
      
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
  
    private void sendFailureEmail(String emailAddressTo){
        String result = null;
        
        String subject_en = "Your payment to Unikbase has been refused or cancelled";
        String subject_fr = "Votre paiement auprès d’Unikbase a été refusé ou annulé";
      
        String message_en = new StringBuilder("We are sorry, but your payment has been refused or cancelled.").append("<br/>")
          						.append("There may have been a problem with the payment information you provided, or your bank account may not have authorized the transaction.").append("<br/>")
								.append("Please check your bank account and credit card information and try again later. If the problem persists, please contact us for further assistance.").append("<br/>")
								.append("We will be happy to assist you.").append("<br/>").append("<br/>")
          						.append("The Unikbase team").append("<br/>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France") .append("<br/>")          
								.append("Contact hello@unikbase.com").toString();
      
		String message_fr = new StringBuilder("Nous sommes désolés, mais votre paiement a été refusé ou annulé.").append("<br/>")
          						.append("Il se peut qu'il y ait eu un problème avec les informations de paiement que vous avez fournies, ou que votre compte bancaire n'ait pas autorisé la transaction.").append("<br/>")
								.append("Veuillez vérifier les informations de votre compte bancaire et de votre carte de crédit, et réessayer ultérieurement. Si le problème persiste, veuillez nous contacter pour plus d'assistance. ").append("<br/>")
								.append("Nous restons à votre disposition.").append("<br/>").append("<br/>")
          						.append("L’équipe Unikbase").append("<br/>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France ").append("<br/>")
								.append("Contact hello@unikbase.com").append("<br/>").toString();



        String htmlMessage = message_en;
        Log.info("Sending failure email to {}", emailAddressTo);
        boolean isFrench = this.sendInFrench();
        try {
            javax.mail.Session mailSession = mailerSessionFactory.getSession();
            MimeMessage emailMessage = new MimeMessage(mailSession);
            emailMessage.setFrom(new InternetAddress("hello@unikbase.com"));
            emailMessage.addRecipient(RecipientType.TO, new InternetAddress(emailAddressTo));
            emailMessage.setSubject(isFrench ? subject_fr : subject_en);
            emailMessage.setText(isFrench ? message_fr : message_en);
            emailMessage.setContent(htmlMessage, "text/html");
            emailMessage.setContentLanguage(isFrench ? new String[]{"fr-FR"} : new String[]{"en-US"});
            emailMessage.setHeader("Accept-Language",(isFrench ? "fr-FR":"en-US"));
            Transport.send(emailMessage);
            result = SUCCESS;
        } catch (Exception e) {
            Log.error("Sending stripe failure email.", e);
            result = "server_error";
            return;
        }
        Log.info("result: {}", result);
    }
  
    public void setSessionId( String sessionId){
        this.sessionId = sessionId;
    }
  
    private boolean sendInFrench(){
    	List<Locale> locales = this.getIntendedLocales();
        Locale locale =  locales != null && locales.size() > 0 ? locales.get(0) : null;
        String languageCode = locale != null ?locale.getLanguage().toLowerCase() : "en";
        return languageCode.equals("fr");
    }
  
     
	
}