[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-capabilities/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-capabilities/job/master/) [![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-capabilities/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-capabilities/job/master/test/?width=800&height=600) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-capabilities&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-capabilities) [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-capabilities&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-capabilities) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.capabilities/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.capabilities%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.capabilities.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.capabilities) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Sling Capabilities Module
=========================

This module is part of the [Apache Sling](https://sling.apache.org) project.

The servlet provided by this module allows for creating Capabilities HTTP endpoints
on a Sling instance: Resources that provide information on which services are available,
version levels etc.

The example configuration below shows the JSON vocabulary used to expose capabilities.

For now, we have one module which provides a `CapabilitiesSource`, that's the [sling-org-apache-sling-capabilities-jcr](https://github.com/apache/sling-org-apache-sling-capabilities-jcr) module which provides information on the JCR repository.

CapabilitiesSource services
----------------------------

Services that implement the `CapabilitiesSource` interface provide capabilities.

Each service must have its own unique namespace, used to split the capabilities in
categories that can be provided separately (see below):

    @ProviderType
    public interface CapabilitiesSource {
        /** @return the namespace to use to group our capabilities.
         *  That name must be unique in a given Sling instance.
         */
        String getNamespace();
	
        /** Return zero to N capabilities, each being represented by
         *  a key/value pair.
         *
         *  Services implementing this interface must be careful to
         *  avoid crossing trust boundaries. They should only expose data that
         * is accessible to the ResourceResolver that's passed
         *  as a parameter.
         *
         * @return a Map of capabilities
         * @param resolver used to establish the user's identity
         * @throws Exception if the capabilities could not be computed.
         */
          Map<String, Object> getCapabilities(ResourceResolver resolver) throws Exception;
    }

This module does not provide any `CapabilitiesSource` service, but an example `SlingServletsSource` is provided in the test code.
    
The sling/capabilities resource type
------------------------------------

To act as an endpoint for capabilities a resource must have the `sling/capabilities`
resource type.

A required property named `namespace_patterns` must be present, containing 1..N Java
regexp patterns to select which capabilities namespaces are exposed by this resource.

As an example, a resource that causes capabilities with the `org\.apache\.sling\.servlets\.test[A|B]` namespace regexp to be output (assuming a `CapabilitiesSource` that provides them is available) can be created as follows:

    curl -u admin:admin \
      -Fsling:resourceType=sling/capabilities \
      -Fnamespace_patterns='org\.apache\.sling\.servlets\.test[A|B]' \
      http://localhost:8080/var/capabilities/caps
