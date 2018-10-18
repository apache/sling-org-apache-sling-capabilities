Sling Capabilities Module
=========================

The servlet provided by this module allows for creating Capabilities HTTP endpoints
on a Sling instance: Resources that provide information on which services are available,
version levels etc.

To avoid exposing more information than strictly needed, this module provides two mechanisms, 
described below, to control where resources that expose capabilities can be found in the Sling 
resource tree, as well as which `CapabilitiesSource` services are considered by those resources.

The example configuration below shows the JSON vocabulary used to expose capabilities.

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

        /** @return zero to N capabilities, each being represented by
         *      a key/value pair.
         * @throws Exception if the capabilities could not be computed.
         */
        Map<String, Object> getCapabilities() throws Exception;
    }
    
The sling/capabilities resource type
------------------------------------

To act as an endpoint for capabilities a resource must have the `sling/capabilities`
resource type.

A required property named `namespace_patterns` must be present, containing 1..N Java
regexp patterns to select which capabilities namespaces are exposed by this resource.

Write access to such resources should be strictly controlled to avoid leaking unwanted
information, along with the CapabilitiesServlet path restrictions described below.

CapabilitiesServlet configuration
---------------------------------
To restrict access to capabilities, the `CapabilitiesServlet` requires a configuration
(see example below) that specifies which paths patterns are acceptable for `sling/capabilities` 
resources.

The idea is to strictly control write access to these paths, so that even if users can create
`sling/capabilities` resources elsewhere they will not expose capabilities data.

If the path of a `sling/capabilities` resource does not match any of the configured patterns,
the servlet returns a 403 status code saying `Invalid path`.

Example configuration
---------------------
This module does not provide any active `CapabilitiesSource` out of the box, but it provides a
`SlingServletsSource` that can be used to exposes which Sling servlets are active, including their
`sling.servlet.*` properties for reference.

With the example configuration below a `sling/capabilities` resource with 
`namespace_patterns='org\.apache\.sling\.servlets\.test[A|B]'` and a path that matches `/var/capabilities/.*`
produces the following output at `/var/capabilities/caps.json` with the resource shown 
below:

    {
      "org.apache.sling.capabilities": {
        "data": {
          "org.apache.sling.servlets.testA": {
            "GetAclServlet_89b2e8f3": {
              "sling.servlet.extensions": "json",
              "sling.servlet.methods": "GET",
              "sling.servlet.resourceTypes": "sling/servlet/default",
              "sling.servlet.selectors": [
                "acl",
                "tidy.acl"
              ]
            }
          },
          "org.apache.sling.servlets.testB": {
            "ChangeUserPasswordServlet_458ad2ce": {
              "sling.servlet.methods": "POST",
              "sling.servlet.resourceTypes": "sling/user",
              "sling.servlet.selectors": "changePassword"
            }
          }
        }
      }
    }

The configured `testC` namespace is omitted due to the `namespace_patterns` property of the resource shown below.

Here's the required configuration, excerpted from `/system/console/status-Configurations`:

    PID = org.apache.sling.capabilities.internal.CapabilitiesServlet
    resourcePathPatterns = [/var/capabilities/.*]

	Factory PID = org.apache.sling.capabilities.defaultsources.SlingServletsSource
	capabilitiesNamespaceSuffix = testA
	servletsLdapFilter = (&(sling.servlet.extensions=json)(sling.servlet.selectors=acl))    
	
	Factory PID = org.apache.sling.capabilities.defaultsources.SlingServletsSource
	capabilitiesNamespaceSuffix = testB
	servletsLdapFilter = (&(sling.servlet.resourceTypes=sling/user)(sling.servlet.selectors=changePassword))

	Factory PID = org.apache.sling.capabilities.defaultsources.SlingServletsSource
	capabilitiesNamespaceSuffix = testC
	servletsLdapFilter = (sling.servlet.extensions=html)

And a resource that then generates the above output can be created with

    curl -u admin:admin \
      -Fsling:resourceType=sling/capabilities \
      -Fnamespace_patterns='org\.apache\.sling\.servlets\.test[A|B]' \
      http://localhost:8080/var/capabilities/caps
