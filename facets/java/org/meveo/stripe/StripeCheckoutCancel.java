package org.meveo.stripe;


import java.util.Map;
import java.util.HashMap;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;


import org.meveo.service.script.Script;
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

public class StripeCheckoutCancel extends Script {
	
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
      
        Stripe.apiKey = "sk_test_51ME7KzF8O6FLWQWJwzBsPG7XXyr1uVSjsRF7J1OkLvusWPUi3aehz6xntJHirHqVdjsdadTHbRF5w9atu3b9QhPk002fWXABem";
        Log.info("===============================================================");
        Log.info("data = "+parameters.get("data").toString());
        Log.info("Called from stripe.com");
        Map<String, String> checkOutInfoMap = this.extractCheckoutInfo(parameters.get("data"));
        String checkoutInfoId = checkOutInfoMap.get("checkoutInfoId");
      	String customerEmail = checkOutInfoMap.get("customerEmail");
        Log.info("checkOutInfoId="+checkoutInfoId);
        Log.info("customerEmail="+customerEmail);
        Log.info("===============================================================");
      
        if(checkoutInfoId != null && checkoutInfoId.length() > 0){
          	Repository defaultRepo = repositoryService.findDefaultRepository();
            try {
                StrCheckoutInfo checkoutInfo = crossStorageApi.find(defaultRepo, checkoutInfoId, StrCheckoutInfo.class);
                checkoutInfo.setResponseCode("400");
                checkoutInfo.setResponse(parameters.get("data").toString());
                crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);            
                Log.info("checkoutInfo instance {} updated", checkoutInfoId);
            } catch (Exception ex) {
                throw new BusinessException(ex);
            }
        }else{
            Log.error("Missing Data - No checkout Info Id is found");
        }
        
        if(customerEmail != null){
            this.sendFailureEmail(customerEmail);
        }
      
	}
  
    private Map<String, String> extractCheckoutInfo(Object paramInfo){
        Map<String, String> checkoutInfo = new HashMap<>();      	
        try{
            Map object = (HashMap)((HashMap)paramInfo).get("object");
            Map metadata = (HashMap)object.get("metadata");
            Map customerDetails =(HashMap)object.get("customer_details");
            checkoutInfo.put("checkoutInfoId",metadata.get("checkoutInfoId").toString());
            checkoutInfo.put("customerEmail",customerDetails.get("email").toString());
        }catch(ClassCastException ex){
          	Log.error("checkoutInfo cannot be extracted ");
        }		
        return checkoutInfo;
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

        try {
            javax.mail.Session mailSession = mailerSessionFactory.getSession();
            MimeMessage emailMessage = new MimeMessage(mailSession);
            emailMessage.setFrom(new InternetAddress("hello@unikbase.com"));
            emailMessage.addRecipient(RecipientType.TO, new InternetAddress(emailAddressTo));
            emailMessage.setSubject(subject_en);
            emailMessage.setText(message_en);
            emailMessage.setContent(htmlMessage, "text/html");
            Transport.send(emailMessage);
            result = SUCCESS;
        } catch (Exception e) {
            Log.error("Sending stripe failure via email failed.", e);
            result = "server_error";
            return;
        }
        Log.info("result: {}", result);
    }
  
    public void setSessionId( String sessionId){
        this.sessionId = sessionId;
    }
	
}