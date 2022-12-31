const strcheckout = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/strcheckout/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const strcheckoutForm = (container) => {
	const html = `<form id='strcheckout-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#strcheckout-form button').onclick = () => {
		const params = {

		};

		strcheckout(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { strcheckout, strcheckoutForm };