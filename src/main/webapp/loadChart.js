	console.log("loadChart hit");
document.addEventListener('DOMContentLoaded', function () {
    const sensorChartCanvas = document.getElementById('sensorChart').getContext('2d');
    let myChart;

    // Fetch sensor data from the servlet
    fetchSensorData();

    // Set an interval to periodically update the chart
    setInterval(fetchSensorData, 5000); // Update every 5 seconds (adjust as needed)

    // Function to fetch sensor data from the servlet
	function fetchSensorData() {
		console.log("FetchSensorData Hit");
        fetch('/HAR_Server/SensorDataServlet')
            .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            	return response.json();
        	})
            .then(data => {
                // Process the data and update the chart
                updateChart(data);
            })
            .catch(error => console.error('Error fetching sensor data:', error));
    }
// Function to update the multi-line chart
	function updateChart(data) {
		console.log("updateChart hit");
	    if (!myChart) {
	        // Initialize the chart if not already initialized
	        myChart = new Chart(sensorChartCanvas, {
	            type: 'line',
	            data: {
	                labels: [],
	                datasets: [
	                    {
	                        label: 'Accelerometer X',
	                        data: [],
	                        borderColor: 'rgba(75, 192, 192, 1)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Accelerometer Y',
	                        data: [],
	                        borderColor: 'rgba(75, 192, 192, 0.7)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Accelerometer Z',
	                        data: [],
	                        borderColor: 'rgba(75, 192, 192, 0.4)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Gyroscope X',
	                        data: [],
	                        borderColor: 'rgba(255, 99, 132, 1)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Gyroscope Y',
	                        data: [],
	                        borderColor: 'rgba(255, 99, 132, 0.7)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Gyroscope Z',
	                        data: [],
	                        borderColor: 'rgba(255, 99, 132, 0.4)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Magnetometer X',
	                        data: [],
	                        borderColor: 'rgba(255, 206, 86, 1)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Magnetometer Y',
	                        data: [],
	                        borderColor: 'rgba(255, 206, 86, 0.7)',
	                        borderWidth: 2,
	                        fill: false
	                    },
	                    {
	                        label: 'Magnetometer Z',
	                        data: [],
	                        borderColor: 'rgba(255, 206, 86, 0.4)',
	                        borderWidth: 2,
	                        fill: false
	                    }
	                ]
	            },
	            options: {
	                scales: {
	                    x: {
	                        type: 'linear',
	                        position: 'bottom'
	                    }
	                }
	            }
	        });
	    }
	
	    // Extract x, y, z values from the received data
	    const xValues = data.map(entry => entry.timestamp);
	
	    // Extract values for each sensor type
	    const accelerometerXValues = data.filter(entry => entry.sensorType === 'ACCELEROMETER').map(entry => entry.x);
	    const accelerometerYValues = data.filter(entry => entry.sensorType === 'ACCELEROMETER').map(entry => entry.y);
	    const accelerometerZValues = data.filter(entry => entry.sensorType === 'ACCELEROMETER').map(entry => entry.z);
		
		console.log("xValues:", xValues);
		console.log("Accelerometer X Values:", accelerometerYValues);
	    
	    const gyroscopeXValues = data.filter(entry => entry.sensorType === 'GYROSCOPE').map(entry => entry.x);
	    const gyroscopeYValues = data.filter(entry => entry.sensorType === 'GYROSCOPE').map(entry => entry.y);
	    const gyroscopeZValues = data.filter(entry => entry.sensorType === 'GYROSCOPE').map(entry => entry.z);
	
	    const magnetometerXValues = data.filter(entry => entry.sensorType === 'MAGNETOMETER').map(entry => entry.x);
	    const magnetometerYValues = data.filter(entry => entry.sensorType === 'MAGNETOMETER').map(entry => entry.y);
	    const magnetometerZValues = data.filter(entry => entry.sensorType === 'MAGNETOMETER').map(entry => entry.z);
	
	    // Update the chart datasets
	    myChart.data.labels = xValues;
	    myChart.data.datasets[0].data = accelerometerXValues;
	    myChart.data.datasets[1].data = accelerometerYValues;
	    myChart.data.datasets[2].data = accelerometerZValues;
	
	    myChart.data.datasets[3].data = gyroscopeXValues;
	    myChart.data.datasets[4].data = gyroscopeYValues;
	    myChart.data.datasets[5].data = gyroscopeZValues;
	
	    myChart.data.datasets[6].data = magnetometerXValues;
	    myChart.data.datasets[7].data = magnetometerYValues;
	    myChart.data.datasets[8].data = magnetometerZValues;
	
	    // Update the chart
	    myChart.update();
}
    // Initial fetch when the DOM is loaded
    fetchSensorData();
});