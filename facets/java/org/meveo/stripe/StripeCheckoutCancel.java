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

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;


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
        String sessionId = "";
        Stripe.apiKey = "sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3";
      
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
        Log.info("===============================================================");
        
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
      
        String message_en = new StringBuilder("<div><img width=\"180px\" src=\"https://unikbase.com/assets/images/logo-u.png\" ></div>").append("<br/>")
          						.append("We are sorry, but your payment has been refused or cancelled.").append("<br/>")
          						.append("There may have been a problem with the payment information you provided, or your bank account may not have authorized the transaction.").append("<br/>")
								.append("Please check your bank account and credit card information and try again later. If the problem persists, please contact us for further assistance.").append("<br/>").append("<br/>").append("<br/>")
								.append("We will be happy to assist you.").append("<br/>").append("<div><img width=\"30px\" src=\"https://unikbase.com/assets/images/logo-oU.png\" >The Unikbase team</div>").append("<br/>")          						
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France") .append("<br/>")          
								.append("Contact hello@unikbase.com").toString();
      
		String message_fr = new StringBuilder("<div><img width=\"180px\" src=\"https://unikbase.com/assets/images/logo-u.png\" ></div>").append("<br/>")
          						.append("Nous sommes désolés, mais votre paiement a été refusé ou annulé.").append("<br/>")
          						.append("Il se peut qu'il y ait eu un problème avec les informations de paiement que vous avez fournies, ou que votre compte bancaire n'ait pas autorisé la transaction.").append("<br/>")
								.append("Veuillez vérifier les informations de votre compte bancaire et de votre carte de crédit, et réessayer ultérieurement. Si le problème persiste, veuillez nous contacter pour plus d'assistance. ").append("<br/>").append("<br/>").append("<br/>")
								.append("Nous restons à votre disposition.").append("<br/>").append("<div><img width=\"30px\" src=\"https://unikbase.com/assets/images/logo-oU.png\" >L’équipe Unikbase</div>").append("<br/>")          						
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France ").append("<br/>")
								.append("Contact hello@unikbase.com").append("<br/>").toString();



        String htmlMessage = message_en;
        Log.info("Sending failure email to {}", emailAddressTo);
        boolean isFrench = this.sendInFrench();
        try {
            javax.mail.Session mailSession = mailerSessionFactory.getSession();
            MimeMessage emailMessage = new MimeMessage(mailSession);
            MimeMultipart content = new MimeMultipart("related");
          
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(new StringBuilder("<body><strong>")
                             .append(message_en).append("</strong>").toString(), "US-ASCII", "html");
          
          	textPart.setContent(textPart, "text/html");
            content.addBodyPart(textPart);          
            MimeBodyPart messageBodyPart = new MimeBodyPart();
         	DataSource fds = new FileDataSource("image");
          
         	messageBodyPart.setDataHandler(new DataHandler(fds));
          	messageBodyPart.setDisposition(MimeBodyPart.INLINE);
          	content.addBodyPart(messageBodyPart);            
            emailMessage.setContent(content);
          
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
  
  
    private boolean sendInFrench(){
    	List<Locale> locales = this.getIntendedLocales();
        Locale locale =  locales != null && locales.size() > 0 ? locales.get(0) : null;
        String languageCode = locale != null ?locale.getLanguage().toLowerCase() : "en";
        return languageCode.equals("fr");
    }
  
     
	
}