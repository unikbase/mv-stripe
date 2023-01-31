const stripeNoPaymentCheckoutSuccess = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/stripeNoPaymentCheckoutSuccess/`, baseUrl);
	if (parameters.customerEmail !== undefined) {
		url.searchParams.append('customerEmail', parameters.customerEmail);
	}

	return fetch(url.toString(), {
		method: 'GET'
	});
}

const stripeNoPaymentCheckoutSuccessForm = (container) => {
	const html = `<form id='stripeNoPaymentCheckoutSuccess-form'>
		<div id='stripeNoPaymentCheckoutSuccess-customerEmail-form-field'>
			<label for='customerEmail'>customerEmail</label>
			<input type='text' id='stripeNoPaymentCheckoutSuccess-customerEmail-param' name='customerEmail'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const customerEmail = container.querySelector('#stripeNoPaymentCheckoutSuccess-customerEmail-param');

	container.querySelector('#stripeNoPaymentCheckoutSuccess-form button').onclick = () => {
		const params = {
			customerEmail : customerEmail.value !== "" ? customerEmail.value : undefined
		};

		stripeNoPaymentCheckoutSuccess(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { stripeNoPaymentCheckoutSuccess, stripeNoPaymentCheckoutSuccessForm };