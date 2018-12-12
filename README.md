[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-capabilities-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-capabilities-1.8) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-capabilities-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-capabilities-1.8/test_results_analyzer/) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)&#32;[![contrib](http://sling.apache.org/badges/status-contrib.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/status/contrib.md)
 
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
