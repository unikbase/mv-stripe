package org.meveo.stripe;


import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeBodyPart;

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


public class StripeCheckoutSuccess extends EndpointScript {
	
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
      
        
        Stripe.apiKey = "sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3";
        
        Map object = (HashMap)((HashMap)parameters.get("data")).get("object");
        String sessionId = object.get("id").toString();            
        Log.info("===============================================================");
      	Log.info("sessionId="+sessionId);
        Log.info("===============================================================");
      
        Session session = null;
        Map<String, String> metadata = null;
        try{
            session = Session.retrieve( sessionId.trim() );
            metadata = session.getMetadata();
        }catch(StripeException ex)  {
            Log.error(ex.getMessage());
            throw new BusinessException( ex);
        }
      
        String checkoutInfoId = metadata.get("checkoutInfoId");
        String customerEmail = metadata.get("customerEmail");
        Log.info("checkoutInfoId="+metadata.get("checkoutInfoId"));
        Log.info("customerEmail="+metadata.get("customerEmail"));
        Log.info("===============================================================");
        
        if(checkoutInfoId != null && checkoutInfoId.length() > 0){
            Repository defaultRepo = repositoryService.findDefaultRepository();
            try {
                StrCheckoutInfo checkoutInfo = crossStorageApi.find(defaultRepo, checkoutInfoId, StrCheckoutInfo.class);
                checkoutInfo.setResponseCode("200");
                checkoutInfo.setResponse(session.toString());
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
  
  
    private void sendSuccessEmail(String emailAddressTo){
        String result = null;
        
        String subject_en = "Your payment to Unikbase has been successfully completed";
        String subject_fr = "Votre paiement auprès d’Unikbase a été effectué avec succès";
      
        String message_en = new StringBuilder("Congratulations! Your payment has been successfully completed. Thank you for your trust and your purchase.").append("<br/>")
          						.append("Your order has been received and will be processed as soon as possible. You will receive a confirmation email when your digital duplicate is ready.").append("<br/>")
								.append("In the meantime, we invite you to download the Unikbase application on your phone which will be used to store your digital duplicate.").append("<br/>")
								.append("We are at your disposal,").append("<br/>").append("<br/>").append("<div><img src=\"cid:2\"></div>").append("<br/>")
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
		boolean isFrench = this.sendInFrench();
        try {
            javax.mail.Session mailSession = mailerSessionFactory.getSession();
            MimeMessage emailMessage = new MimeMessage(mailSession);
            /*MimeMultipart content = new MimeMultipart("related");
            MimeBodyPart textPart = new MimeBodyPart();
          
			textPart.setText(new StringBuilder("<html><head>").append("<title>This is not usually displayed</title>").append("</head>")
                             .append("<body><div><img src=\"cid:1\"></div><div><strong>").append(isFrench ? message_fr : message_en).append("</strong></div>").toString(), "US-ASCII", "html");

   			content.addBodyPart(textPart);

			MimeBodyPart imagePart = new MimeBodyPart();
			imagePart.attachFile("resources/teapot.jpg");
			imagePart.setContentID("<1>");
			imagePart.setDisposition(MimeBodyPart.INLINE);
			content.addBodyPart(imagePart);

			MimeBodyPart imagePart = new MimeBodyPart();
			imagePart.attachFile("resources/teapot2.jpg");
			imagePart.setContentID("<2>");
			imagePart.setDisposition(MimeBodyPart.INLINE);
			content.addBodyPart(imagePart);*/

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
            Log.error("Sending stripe success via email failed.", e);
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

 