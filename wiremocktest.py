import json
from haralyzer import HarParser, HarPage
import os

def har_to_wiremock(har_file_path, output_dir):
    # Load the HAR file
    with open(har_file_path, 'r') as f:
        har_parser = HarParser(json.load(f))

    # Ensure the output directory exists
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Iterate through each entry in the HAR file
    for entry in har_parser.har_data['log']['entries']:
        request = entry['request']
        response = entry['response']

        method = request['method']
        url = request['url']
        status = response['status']
        body = response['content'].get('text', '')

        # Create the WireMock stub
        stub = {
            "request": {
                "method": method,
                "url": url
            },
            "response": {
                "status": status,
                "body": body
            }
        }

        # Write the stub to a JSON file
        stub_filename = f"stub_{method}_{hash(url)}.json"
        with open(os.path.join(output_dir, stub_filename), 'w') as stub_file:
            json.dump(stub, stub_file, indent=4)

        print(f"Stub created for: {url}")

# Specify the HAR file path and output directory for the WireMock stubs
har_file_path = 'path/to/your.har'
output_dir = 'path/to/output/stubs'
har_to_wiremock(har_file_path, output_dir)
