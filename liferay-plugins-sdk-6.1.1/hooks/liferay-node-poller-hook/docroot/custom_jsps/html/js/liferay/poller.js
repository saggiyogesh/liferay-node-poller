AUI.add(
    'liferay-poller',
    function (A) {
        var Util = Liferay.Util;

        var _browserKey = Util.randomInt();

        var _encryptedUserId = null;

        var _portlets = {};

        var _registeredPortlets = [];

        var _getEncryptedUserId = function () {
            return _encryptedUserId;
        };

        var _nodePollerURL;

        var _metaData = {
            browserKey: _browserKey,
            companyId: themeDisplay.getCompanyId(),
            isSignedIn: themeDisplay.isSignedIn(),
            userId: themeDisplay.getUserId()
        };

        var print = function (msg) {
        	console.log(msg)
//            console && console.log && console.log(msg) || alert(msg);
        };

        var _sockets = {};
        
        var _insertMetaData = function(obj) {
        	_metaData.timestamp = (new Date()).getTime();
			return A.mix(obj, _metaData);
		};

        var _callListener = function (key, e) {
        	var portlet = _portlets[key];
        	portlet.listener.call(portlet.scope || Poller, A.JSON.parse(e.data).data);
        };

        var _initPolling = function (key) {
        	_metaData.timestamp = (new Date()).getTime();
        	_metaData.initialRequest = "true";
            return JSON.stringify({
            	meta: _metaData,
            	send: {id: key, action: "init"}
            });
        };

        var _openSocket = function (key) {
            if (!_nodePollerURL || _sockets.hasOwnProperty(key)) {
                return;
            }
            var socket = new SockJS(_nodePollerURL + "echo");
            console.log(socket)
            socket.onopen = function () {
                print(key + ' [*] open', socket.protocol);
                socket.send(_initPolling(key));
            };
            socket.onmessage = function (e) {
                _callListener(key, e);
            };
            socket.onclose = function () {
                print(key + ' [*] close');
            };

            _sockets[key] = socket;
        };
        

        var Poller = {
            init: function (options) {
                if (!options.nodePollerURL) {
                    throw new Error("Node poller url missing.");
                }
                var instance = this;
                instance.setEncryptedUserId(options.encryptedUserId);
                instance.setNodePollerURL(options.nodePollerURL);
                A.each(_portlets, function(portlet, key) {
                	console.log(key);
                	_openSocket(key);					
				});
                
                if(themeDisplay.isSignedIn()){
                	Liferay.bind('sessionExpired', function(event) {
                		print(event);
                		A.each(_sockets, function(socket, key){
                			socket.close();
                		});
                		
                	});
                }
                
            },
            addListener: function (key, listener, scope) {
                _portlets[key] = {
                    listener: listener,
                    scope: scope
                };

                if (A.Array.indexOf(_registeredPortlets, key) == -1) {
                    _registeredPortlets.push(key);
                }
                
                _openSocket(key);

            },
            removeListener: function (key) {
                var instance = this;

                if (_portlets.hasOwnProperty(key)) {
                    delete _portlets[key];
                    _sockets[key].close();
                    delete _sockets[key];
                }

                var index = A.Array.indexOf(_registeredPortlets, key);

                if (index > -1) {
                    _registeredPortlets.splice(index, 1);
                }
            },
            setEncryptedUserId: function (encryptedUserId) {
                _encryptedUserId = encryptedUserId;
            },
            setNodePollerURL: function (nodePollerURL) {
                _nodePollerURL = nodePollerURL;
            },
            submitRequest: function (key, data, chunkId) {
                if (_portlets.hasOwnProperty(key)) {
                    /*for (var i in data) {
                     var content = data[i];

                     if (content.replace) {
                     content = content.replace(_openCurlyBrace, _escapedOpenCurlyBrace);
                     content = content.replace(_closeCurlyBrace, _escapedCloseCurlyBrace);

                     data[i] = content;
                     }
                     }*/

                    _metaData.timestamp = (new Date()).getTime();

                    var requestData = {
                        id: key,
                        action:"send",
                        data: data
                    };

                    _sockets[key].send(A.JSON.stringify({
                    	meta: _metaData,
                    	send: requestData
                    }));
                }
            }

        };

        Liferay.Poller = Poller;


    },
    '',
    {
        requires: ['aui-base', 'io', 'json']
    }
);