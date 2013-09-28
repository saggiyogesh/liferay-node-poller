Liferay node poller
===================

Liferay node poller consists a hook available in plugins sdk and a nodejs/sockjs server named as NodePoller.

## Installation & Configuration:
* Install node.js 
* Clone the repo:

    ```
    git clone git@github.com:saggiyogesh/liferay-node-poller.git
    ```
    
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
