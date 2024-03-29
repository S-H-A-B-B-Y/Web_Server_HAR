/*function generateGraph() {
    // Get the file name from the input field
    const fileName = document.getElementById('fileName').value;

    const absolutePath = "http://localhost:8080/HAR_Server/"; // Update this with your absolute path

    fetch(`${absolutePath}${fileName}.csv`)
        .then(response => response.text())
        .then(data => {
            // Parse the CSV data
            const parsedData = parseCSV(data);

            // Call the function to build the line chart
            buildLineChart(parsedData);
        })
        .catch(error => console.error('Error fetching CSV file:', error));
}*/
// Function to parse CSV data into an array
/*function parseCSV(csvData) {
    const rows = csvData.split('\n');
    const parsedData = [];

    rows.forEach(row => {
        const columns = row.split(',');
        const timestamp = parseInt(columns[2].trim()); // Assuming timestamp is at index 2
        const sensorType = columns[3].trim().split(':')[0]; // Assuming sensor type is at index 3
        const x = parseFloat(columns[3].trim().split(':')[1]);
        const y = parseFloat(columns[4].trim());
        const z = parseFloat(columns[5].trim());
        parsedData.push({ timestamp, sensorType, x, y, z });
    });

    return parsedData;
}*/
// Function to build the line chart using Chart.js library
/*function buildLineChart(data) {
    // Get unique timestamps
    const timestamps = [...new Set(data.map(entry => entry.timestamp))];

    // Separate data by sensor type
    const accelerometerData = data.filter(entry => entry.sensorType === 'ACCELEROMETER');
    const gyroscopeData = data.filter(entry => entry.sensorType === 'GYROSCOPE');
    const magnetometerData = data.filter(entry => entry.sensorType === 'MAGNETOMETER');

    // Initialize datasets
    const datasets = [];

    // Populate datasets with x, y, z values for each sensor type
    datasets.push(createDataset(accelerometerData, 'Accelerometer'));
    datasets.push(createDataset(gyroscopeData, 'Gyroscope'));
    datasets.push(createDataset(magnetometerData, 'Magnetometer'));

    // Get the canvas element
    const canvas = document.getElementById('sensorChart');

    // Create a new Chart instance
    const chart = new Chart(canvas, {
        type: 'line',
        data: {
            labels: timestamps,
            datasets: datasets
        },
        options: {
            scales: {
                x: {
                    type: 'linear',
                    position: 'bottom',
                    title: {
                        display: true,
                        text: 'Timestamp'
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: 'Sensor Values'
                    }
                }
            }
        }
    });
}
*/

function generateGraph() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (!file) {
        console.error('No file selected.');
        return;
    }

    const reader = new FileReader();

    reader.onload = function(event) {
        const csvData = event.target.result;

        // Parse the CSV data
        const parsedData = parseCSV(csvData);

        // Call the function to build the line chart
        //buildLineChart(parsedData);
        drawChart(parsedData);
    };

    reader.onerror = function(event) {
        console.error('Error reading the file:', event.target.error);
    };

    reader.readAsText(file);
}
function parseCSV(csvData) {
    const rows = csvData.split('\n');
    const parsedData = [];

    rows.forEach(row => {
        if (!row.trim()) return; // Skip empty rows
        const columns = row.split(', ');
        const timestamp = parseInt(columns[2].split(':')[1].trim()); // Extracting timestamp from the third column
        const sensorType = columns[3].split(':')[1].split(',')[0].trim(); // Extracting sensor type from the fourth column
        const values = columns[3].split(':')[1].split(',').slice(1).map(value => parseFloat(value.trim())); // Extracting sensor values from the fourth column
        const [x, y, z] = values;
        parsedData.push({ timestamp, sensorType, x, y, z });
    });

    return parsedData;
}



// Function to create a dataset for a sensor type
function createDataset(data, label) {
    const xValues = data.map(entry => entry.x);
    const yValues = data.map(entry => entry.y);
    const zValues = data.map(entry => entry.z);

    return {
        label: label,
        data: {
            x: xValues,
            y: yValues,
            z: zValues
        },
        borderColor: getRandomColor(),
        borderWidth: 2,
        fill: false
    };
}

// Function to generate a random color
function getRandomColor() {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}
function drawChart(data) {
    // Load the Google Charts library
    google.charts.load('current', { packages: ['corechart'] });
  
    // Set a callback to run when the Google Charts library is loaded
    google.charts.setOnLoadCallback(function () {
        // Create a new DataTable
        var dataTable = new google.visualization.DataTable();
        dataTable.addColumn('number', 'Timestamp');
        dataTable.addColumn('number', 'Accelerometer Y');
        // Add data rows to the DataTable
        data.forEach(entry => {
            dataTable.addRow([entry.timestamp, entry.y]);
        });
        
        // Set chart options
        var options = {
            title: 'Sensor Data',
            curveType: 'function',
            legend: { position: 'bottom' }
        };

        // Instantiate and draw the chart
        var chart = new google.visualization.LineChart(document.getElementById('sensorChart'));
        chart.draw(dataTable, options);
    });
}
