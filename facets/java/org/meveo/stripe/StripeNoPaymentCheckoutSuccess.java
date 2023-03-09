package org.meveo.stripe;


import java.util.*;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.email.service.EmailService;
import org.meveo.googlesheet.service.GoogleSheetViaFormService;


public class StripeNoPaymentCheckoutSuccess extends Script {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeNoPaymentCheckoutSuccess.class);
    private String customerEmail = null;
    private String tpkId = null;      
    private String result;

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
        
        
        Log.info("===============================================================");
        Log.info("tpkId="+tpkId);
      	Log.info("customerEmail="+customerEmail);
        Log.info("===============================================================");
      
        this.writeToSheet();
      
        if(customerEmail != null){
            this.sendSuccessEmail(customerEmail);
        }
      
      	result = "SUCCESS";
	}
  
    private void sendSuccessEmail(String emailAddressTo) throws BusinessException{
        Map<String, Object> mapping = new HashMap<>();
      	mapping.put("emailType", "FREE_ORDER");
      	mapping.put("emailAddressTo", emailAddressTo);
      	mapping.put("mapping", new HashMap());
      
        Script emailService = new EmailService();  
        emailService.execute(mapping);
    }
  
    private void writeToSheet(){
        GoogleSheetViaFormService googleSheetService = new GoogleSheetViaFormService();
        try {         	
          	googleSheetService.setOrderId(this.tpkId != null ? Long.valueOf(this.tpkId): null);
          	googleSheetService.setThreeDScan("false");
          	googleSheetService.setEmail(this.customerEmail);
            googleSheetService.setStatus("A traiter TPK");            
            googleSheetService.setDateCreationTPK(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
          	googleSheetService.execute(null);
        } catch(Exception ex) {
            Log.error(googleSheetService.toString(), ex);
        }
    }
  
    public void setCustomerEmail(String customerEmail){
        this.customerEmail = customerEmail;
    }
  
    public void setTpkId(String tpkId){
        this.tpkId = tpkId;
    }
  
  	public String getResult() {
		return result;
	}	
}