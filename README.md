Liferay node poller
===================

Current Liferay poller implementation is AJAX based, it recusively fires send and receive ajax request. 

This implementation uses node/sockjs to use HTML5 websockets for supported browsers and long polling for other browsers.

Liferay node poller consists a hook available in plugins sdk and a nodejs/sockjs server named as NodePoller.

## Installation & Configuration:
* Install [node.js](http://nodejs.org/download/) 
* Clone the repo:

    ```
    git clone git@github.com:saggiyogesh/liferay-node-poller.git
    ```
    
    or [download](https://github.com/saggiyogesh/liferay-node-poller/releases/tag/1.0) the source.
### NodePoller Configuration:
* In NodePoller folder install the dependencies:


    
    ```
    npm install   
    ```

* Edit the Liferay server url in `poller.properties`  
* Run nodejs server, listening on port 9999.

    ```
    node server 
    ```
    
### Liferay Configuration
* In `portal-ext.properties` file edit the NodePoller url.
* Deploy Liferay chat portlet from market place.
* Deploy 'liferay-node-poller-hook-6.1.1.1.war' available in dist folder of plugins sdk.

Note: Hook should be deployed at last.

Now Liferay is configured with Node poller. 

To remove this, simply undeploy liferay-node-poller-hook from Update Manager in Liferay.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/saggiyogesh/liferay-node-poller/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

