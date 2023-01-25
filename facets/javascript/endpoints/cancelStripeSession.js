const cancelStripeSession = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/cancelStripeSession/${parameters.sessionId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const cancelStripeSessionForm = (container) => {
	const html = `<form id='cancelStripeSession-form'>
		<div id='cancelStripeSession-sessionId-form-field'>
			<label for='sessionId'>sessionId</label>
			<input type='text' id='cancelStripeSession-sessionId-param' name='sessionId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const sessionId = container.querySelector('#cancelStripeSession-sessionId-param');

	container.querySelector('#cancelStripeSession-form button').onclick = () => {
		const params = {
			sessionId : sessionId.value !== "" ? sessionId.value : undefined
		};

		cancelStripeSession(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { cancelStripeSession, cancelStripeSessionForm };