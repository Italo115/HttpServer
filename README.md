# HTTP Server with Gzip Compression Support

## Overview

This project implements a simple HTTP server in Java. The server supports the following features:

- Handling HTTP GET and POST requests
- Serving static files from a specified directory
- Echoing back user-provided strings
- Returning the `User-Agent` header from the client request
- Gzip compression for responses when requested by the client

## Features

1. **GET /echo/{str}**
    - Responds with the provided string.
    - Supports gzip compression if requested by the client.

2. **GET /user-agent**
    - Responds with the `User-Agent` header from the client's request.
    - Supports gzip compression if requested by the client.

3. **GET /files/{filename}**
    - Serves the requested file from the specified directory.
    - Supports gzip compression if requested by the client.

4. **POST /files/{filename}**
    - Saves the request body as a file in the specified directory.
    - Returns a `201 Created` response if successful.
    - Returns a `404 Not Found` response if the target is not `/files/{filename}`.

## Running the Server

### Prerequisites

- Java Development Kit (JDK) 8 or higher

### Compilation

Compile the server code with the following command:

```sh
javac Main.java RequestHandler.java
```

### Execution

Run the server with the specified directory for storing and serving files:

```sh
java Main --directory /path/to/directory
```

### Example

```sh
java Main --directory /tmp/
```

## Usage

### GET /echo/{str}

**Request:**

```sh
curl -v -H "Accept-Encoding: gzip" http://localhost:4221/echo/abc
```

**Response:**

```
HTTP/1.1 200 OK
Content-Encoding: gzip
Content-Type: text/plain
Content-Length: <compressed size>

<compressed body>
```

### GET /user-agent

**Request:**

```sh
curl -v -H "Accept-Encoding: gzip" http://localhost:4221/user-agent
```

**Response:**

```
HTTP/1.1 200 OK
Content-Encoding: gzip
Content-Type: text/plain
Content-Length: <compressed size>

<compressed User-Agent string>
```

### GET /files/{filename}

**Request:**

```sh
curl -v -H "Accept-Encoding: gzip" http://localhost:4221/files/example.txt
```

**Response:**

```
HTTP/1.1 200 OK
Content-Encoding: gzip
Content-Type: application/octet-stream
Content-Length: <compressed size>

<compressed file content>
```

### POST /files/{filename}

**Request:**

```sh
curl -X POST --data "file content" http://localhost:4221/files/example.txt
```

**Response:**

```
HTTP/1.1 201 Created
```

## Code Structure

### Main.java

The `Main` class initializes the server and listens for incoming connections. It delegates the handling of each connection to a new `RequestHandler` thread.

### RequestHandler.java

The `RequestHandler` class handles the processing of HTTP requests. It:
- Parses the request line and headers
- Handles GET and POST requests
- Supports gzip compression for responses

## Gzip Compression

The server supports gzip compression for responses if the client includes `gzip` in the `Accept-Encoding` header. The compressed response is sent with the `Content-Encoding: gzip` header.

## Error Handling

The server handles various error scenarios and responds with appropriate HTTP status codes:
- `400 Bad Request`: Sent when the request is malformed.
- `404 Not Found`: Sent when the requested resource is not found or the target is invalid.

## Logging

The server logs accepted connections and any I/O exceptions to the console for debugging purposes.

## Future Enhancements

- Add support for more HTTP methods (e.g., PUT, DELETE)
- Implement additional compression schemes
- Improve error handling and logging
- Add support for HTTPS

## License

---

This README provides a comprehensive overview of the HTTP server, its features, usage instructions, and code structure. It also outlines future enhancements and licensing information.