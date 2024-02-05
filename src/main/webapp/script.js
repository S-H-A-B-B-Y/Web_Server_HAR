// Common function to make fetch requests
function makeFetchRequest(url, method, callback) {
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        // Add body or modify headers as needed
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Failed to make a request to ${url}`);
        }
        return response.text();
    })
    .then(data => {
        callback(data);
    })
    .catch(error => {
        console.error(`Error making a request to ${url}:`, error);
    });
}

// Function to start the server
function startServer() {
    if (document.getElementById('status').innerText === 'Server not running') {
        document.getElementById('status').innerText = "Server running";
        makeFetchRequest('/HAR_Server/StatusCheckServlet?action=start', 'GET', alert);
        console.log("StatusRequest done");
        makeFetchRequest('/HAR_Server/SensorDataServlet', 'POST', () => {});
        console.log("Server Request made");
    }
}

// Function to stop the server
function stopServer() {
    document.getElementById('status').innerText = 'Server not running';
    makeFetchRequest('/HAR_Server/StatusCheckServlet?action=stop', 'GET', alert);
    console.log("StatusRequest stop done");
    //makeFetchRequest('/HAR_Server/SensorDataServlet?action=stop', 'GET', () => {});
    //console.log("Server stop Request made");
}

