package org.meveo.stripe;

import javax.inject.Inject;
import java.util.Map;
import java.time.Instant;
import java.util.HashMap;

import org.meveo.service.script.Script;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

//import org.meveo.service.admin.impl.credentials.CredentialHelperService;
//import org.meveo.model.admin.MvCredential;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.StrCheckoutInfo;

import org.meveo.api.rest.technicalservice.EndpointScript;

public class CheckoutSessionScript extends EndpointScript {

	@Inject
	private CrossStorageApi crossStorageApi;

	@Inject
	private RepositoryService repositoryService;

	//@Inject
	//private CredentialHelperService credentialHelperService;

	private static final Logger Log = LoggerFactory.getLogger(CheckoutSessionScript.class);

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		Log.info("received {}", parameters);
		StrCheckoutInfo checkoutInfo = new StrCheckoutInfo();
		checkoutInfo.setCreationDate(Instant.now());

		// as long as we have an email we process the payment
		if (parameters.containsKey("email")) {
			checkoutInfo.setEmail(parameters.get("email"));
		} else {
			endpointResponse.setStatus(400);
			endpointResponse.setErrorMessage("missing email");
		}

		Map<String, String> inputInfo = new HashMap<>();
		if (parameters.containsKey("tpk_id")) {
			inputInfo.put("tpk_id", parameters.get("tpk_id"));
		}
		if (parameters.containsKey("value")) {
			inputInfo.put("value", parameters.get("value"));
		}
		if (parameters.containsKey("token")) {
			inputInfo.put("token", parameters.get("token"));
		}
		checkoutInfo.setInputInfo(inputInfo);

		Repository defaultRepo = repositoryService.findDefaultRepository();
		try {
			String uuid = crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);
			Log.info("checkoutInfo instance {} created",uuid);
		} catch (Exception ex) {
			throw new BusinessApiException(ex);
		}


		//retrieve apiKey from credential
		//MvCredential credential = credentialHelperService.getCredential("stripe.com");
		//if(credential==null){
		//	Log.severe("stripe.com credential not found");
		//	throw new BusinessException("technical error");
		//}
		Stripe.apiKey = "sk_test_51MDclnDFTHQUCZxe4PMxI7uwtGbuYcsw1MMX2jpdEzdjT395Ebq2DM6PwDjPo07MHXkbLDEMWuAlvJwVt7F3Cyf300FpV5C2QC";

		SessionCreateParams params =
          SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("onboarding.unikbase.com/success.html")
            .setCancelUrl("onboarding.unikbase.com/cancel.html")
            .setAutomaticTax(
              SessionCreateParams.AutomaticTax.builder()
                .setEnabled(true)
                .build())
            .addLineItem(
              SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPrice("price_1MDdCZDFTHQUCZxeVopcF11X")
                .build())
            .build();
      Session session = Session.create(params);
	  Log.info("session {}", session);
	  endpointResponse.setOutput((""+session.getUrl()).getBytes());
	}

}