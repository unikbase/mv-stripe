package org.meveo.stripe;


import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.email.service.EmailService;

public class StripeNoPaymentCheckoutSuccess extends Script {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeNoPaymentCheckoutSuccess.class);
    private String customerEmail = null;
      
    private String result;

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
        
        Log.info("===============================================================");
      	Log.info("customerEmail="+customerEmail);
        Log.info("===============================================================");
      
        if(customerEmail != null){
            this.sendSuccessEmail(customerEmail);
        }
      
      	result = "SUCCESS";
	}
  
    private void sendSuccessEmail(String emailAddressTo) throws BusinessException{
        Map<String, Object> mapping = new HashMap<>();
      	mapping.put("emailType", "FREE_ORDER");
      	mapping.put("emailAddressTo", emailAddressTo);
      	mapping.put("mapping", Collections.emptyMap());
      
        Script emailService = new EmailService();  
        emailService.execute(mapping);
    }
  
    public void setCustomerEmail(String customerEmail){
        this.customerEmail = customerEmail;
    }
  
  	public String getResult() {
		return result;
	}	
}