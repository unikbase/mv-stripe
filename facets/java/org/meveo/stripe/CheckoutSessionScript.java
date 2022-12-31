package org.meveo.stripe;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckoutSessionScript extends Script {
  
  	private static final Logger Log = LoggerFactory.getLogger(CheckoutSessionScript.class);
	
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
      Log.info("received {}",parameters);
		
	}
	
}