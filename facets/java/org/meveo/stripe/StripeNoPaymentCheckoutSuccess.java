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
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import org.meveo.service.script.Script;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.MailerSessionFactory;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;


public class StripeNoPaymentCheckoutSuccess extends EndpointScript {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeNoPaymentCheckoutSuccess.class);
    private static final String SUCCESS = "Nous vous remercions de votre commande! Si vous avez des questions, contactez nous par email: orders@unikbase.com";
    private String customerEmail = null;
  
    @Inject
    private MailerSessionFactory mailerSessionFactory;
  
    private String result="";

	public String getResult() {
		return result;
	}
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
        
        Log.info("===============================================================");
      	Log.info("customerEmail="+customerEmail);
        Log.info("===============================================================");
      
        if(customerEmail != null){
            this.sendSuccessEmail(customerEmail);
        }
      
	}
  
  
    private void sendSuccessEmail(String emailAddressTo){
        
        String subject_en = "Your payment to Unikbase has been successfully completed";
        String subject_fr = "Votre paiement auprès d’Unikbase a été effectué avec succès";
      
        String message_en = new StringBuilder("<div><img width=\"180px\" src=\"https://unikbase.com/assets/images/logo-u.png\" ></div>").append("<br/>")
          						.append("Congratulations! Your payment has been successfully completed. Thank you for your trust and your purchase.").append("<br/>").append("<br/>")
          						.append("Your order has been received and will be processed as soon as possible. You will receive a confirmation email when your digital duplicate is ready.").append("<br/>").append("<br/>")
								.append("In the meantime, we invite you to download the Unikbase application on your phone which will be used to store your digital duplicate.").append("<br/>").append("<br/>").append("<br/>")
								.append("We are at your disposal,").append("<br/>").append("<div><img width=\"30px\" src=\"https://unikbase.com/assets/images/logo-oU.png\" >The Unikbase team</div>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France") .append("<br/>")          
								.append("Contact hello@unikbase.com").toString();
      
		String message_fr = new StringBuilder("<div><img width=\"180px\" src=\"https://unikbase.com/assets/images/logo-u.png\" ></div>").append("<br/>")
          						.append("Félicitations ! Votre paiement a été effectué avec succès. Nous vous remercions de votre confiance et de votre achat.").append("<br/>").append("<br/>")
          						.append("Votre commande a été reçue et sera traitée dans les plus brefs délais. Vous recevrez un email de confirmation lorsque votre double numérique sera prêt.").append("<br/>").append("<br/>")
								.append("D’ici là nous vous invitons dès à présent à télécharger sur votre téléphone l’application Unikbase qui servira à stocker votre double numérique. ").append("<br/>").append("<br/>").append("<br/>")
								.append("Nous restons à votre disposition,").append("<br/>")
          						.append("<div><img width=\"30px\" src=\"https://unikbase.com/assets/images/logo-oU.png\" > L’équipe Unikbase</div>")
          						.append("+ Unikbase, 320 rue Saint-Honoré 75001 Paris, France ").append("<br/>")
								.append("Contact hello@unikbase.com").append("<br/>").toString();




        String htmlMessage = message_en;
        Log.info("Sending success Email to {}", emailAddressTo);
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
            emailMessage.setContentLanguage(isFrench ? new String[]{"fr-FR"} : new String[]{"en-US"});
            emailMessage.setHeader("Accept-Language",(isFrench ? "fr-FR":"en-US"));
            Transport.send(emailMessage);
            this.result = SUCCESS;
        } catch (Exception e) {
            Log.error("Sending stripe success via email failed.", e);
            result = "server_error";
            return;
        }
        Log.info("result: {}", result);
    }
  
    public void setCustomerEmail(String customerEmail){
        this.customerEmail = customerEmail;
    }
  
    private boolean sendInFrench(){
    	List<Locale> locales = this.getIntendedLocales();
        Locale locale =  locales != null && locales.size() > 0 ? locales.get(0) : null;
        String languageCode = locale != null ?locale.getLanguage().toLowerCase() : "en";
        return languageCode.equals("fr");
    }
	
}

 