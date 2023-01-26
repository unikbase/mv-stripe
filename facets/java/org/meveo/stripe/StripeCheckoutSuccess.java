package org.meveo.stripe;


import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
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


public class StripeCheckoutSuccess extends Script {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeCheckoutSuccess.class);
    private static final String SUCCESS = "success";


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
        //.get("metaData").get("checkoutInfoId")
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
                checkoutInfo.setResponseCode("200");
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
            this.sendSuccessEmail(customerEmail);
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
  
    private void sendSuccessEmail(String emailAddressTo){
        String result = null;
        
        String subject_en = "Your payment to Unikbase has been successfully completed";
        String subject_fr = "Votre paiement auprès d’Unikbase a été effectué avec succès";
      
        String message_en = new StringBuilder("Congratulations! Your payment has been successfully completed. Thank you for your trust and your purchase.").append("<br/>")
          						.append("Your order has been received and will be processed as soon as possible. You will receive a confirmation email when your digital duplicate is ready.").append("<br/>")
								.append("In the meantime, we invite you to download the Unikbase application on your phone which will be used to store your digital duplicate.").append("<br/>")
								.append("We are at your disposal,").append("<br/>").append("<br/>").append("<br/>")
          						.append("The Unikbase team").append("<br/>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France") .append("<br/>")          
								.append("Contact hello@unikbase.com").toString();
      
		String message_fr = new StringBuilder("Félicitations ! Votre paiement a été effectué avec succès. Nous vous remercions de votre confiance et de votre achat.").append("<br/>")
          						.append("Votre commande a été reçue et sera traitée dans les plus brefs délais. Vous recevrez un email de confirmation lorsque votre double numérique sera prêt.").append("<br/>")
								.append("D’ici là nous vous invitons dès à présent à télécharger sur votre téléphone l’application Unikbase qui servira à stocker votre double numérique. ").append("<br/>")
								.append("Nous restons à votre disposition,").append("<br/>").append("<br/>")
          						.append("L’équipe Unikbase").append("<br/>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France ").append("<br/>")
								.append("Contact hello@unikbase.com").append("<br/>").toString();




        String htmlMessage = message_en;
        Log.info("Sending success Email to {}", emailAddressTo);

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
            Log.error("Sending stripe success via email failed.", e);
            result = "server_error";
            return;
        }
        Log.info("result: {}", result);
    }
	
}

 