# federated-login-ext

Liferay ext plug-in to support SAML and STORK federations. The
extension does not manage the SAML messages directly but delegate the
message encoding/decoding to an external server, generally apache.

## Requirements

### SAML Federation

To perform authentication using SAML, Liferay has to be executed
behind apache (or similar service) configured to perform SAML
authentication. The attributes has to be provided to the application
server in order for the module to read them. A common scenario is to
use apache with [mod_shibboleth](https://shibboleth.net/products/),
which is already available in many linux distribution. Apache will
communicate with the application server using mod_proxy_ajp or other
proxies.

### STORK federation

The STORK module relies on [opensmal-java
libraries](https://shibboleth.net/products/). Please, download and
make available them in your application context following the
guideline for your application server.



## Installation Note


The plug-in work only for Liferay 6.1. It is an ext so after the installation the source code of liferay is modified and it cannot be reverted to the original so before to install create a backup of your current installation

