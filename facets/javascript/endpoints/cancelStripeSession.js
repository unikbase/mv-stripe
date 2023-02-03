import EndpointInterface from "#{API_BASE_URL}/api/rest/endpoint/EndpointInterface.js";

// the request schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this is used to validate and parse the request parameters
const requestSchema = {
  "title" : "cancelStripeSessionRequest",
  "id" : "cancelStripeSessionRequest",
  "default" : "Schema definition for cancelStripeSession",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "sessionId" : {
      "title" : "sessionId",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// the response schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this could be used to parse the result
const responseSchema = {
  "title" : "cancelStripeSessionResponse",
  "id" : "cancelStripeSessionResponse",
  "default" : "Schema definition for cancelStripeSession",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object"
}

// should contain offline mock data, make sure it adheres to the response schema
const mockResult = {};

class cancelStripeSession extends EndpointInterface {
	constructor() {
		// name and http method, these are inserted when code is generated
		super("cancelStripeSession", "POST");
		this.requestSchema = requestSchema;
		this.responseSchema = responseSchema;
		this.mockResult = mockResult;
	}

	getRequestSchema() {
		return this.requestSchema;
	}

	getResponseSchema() {
		return this.responseSchema;
	}
}

export default new cancelStripeSession();