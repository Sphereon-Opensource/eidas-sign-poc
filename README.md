<!--suppress HtmlDeprecatedAttribute -->
<h1 align="center">
  <br>
  <a href="https://www.sphereon.com"><img src="https://sphereon.com/content/themes/sphereon/assets/img/logo.svg" alt="Sphereon" width="400"></a>
  <br>eIdas Bridge Sign API<br>
    Proof of Concept
  <br>
</h1>

---

**Warning:** This API is a Proof of Concept which was used to learn and implement an eIdas capable VC bridge. A new production implementation that is mostly open-source and which can be ran in our commercial offering is being created. You can use this code for inspiration. This sign PoC should be ran in conjuction with the eidas-vc-bridge-poc. 

**Do not use this PoC in production settings!**

---

This component needs to be run together with the edias-vc-bridge-poc API. That API handles the Verifiable Credentials, this API handles generic signing.

# API documentation
Please see https://sphereon-opensource.github.io/eidas-sign-poc/ for the API documentation once the API is up and running you can also find the documentation at http://localhost:21762/docs

# Prerequisite

In order to build the application you will need at least Java version 11 and maven installed. The API requires a mongo database as well.


## Running the container and configuration
You can run the container using the docker command or using the docker-compose command after building the software. Alternatively you can run the docker container by using the latest version published in dockerhub (see below).

If you run the docker-compose file, a Mongo database will be setup for you in a separate container

### Environment variables
Currently NA

# Running a provided Docker container
This is the simplest option. It runs the API as a docker container, without the need for building the software.

You do need to have a mongo database available listening at localhost:27017

``
docker run -p 21762:21762 sphereon/eidas-sign-poc
``

Point your browser to http://localhost:21762/docs to doublecheck that the API is running correctly


# Building and running the source code

## Build (native Java)
Maven build:

	mvn clean install

### Run (Docker compose)
``
docker-compose docker-compose.yml
``

Point your browser to http://localhost:21762/docs

### Run (Docker)

Ensure you have a mongo DB available

```
docker build . -t sphereon/eidas-sign-poc
docker run -p 21762:21762 sphereon/eidas-sign-poc
```
Point your browser to http://localhost:21762/docs
