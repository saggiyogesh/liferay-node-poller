var express = require('express');
var sockjs = require('sockjs');
var http = require('http');
var request = require('request');
var util = require("util");
var Connections = require("./Connections");
var Properties = require("properties-parser");

// 1. Echo sockjs server
var sockjs_opts = {sockjs_url: "/js/sockjs-0.3.min.js"};

function showInConsole(str) {
    var fileStr = new Error().stack.split('\n')[3];
    //fileStr = fileStr.substring(fileStr.lastIndexOf("/") + 1, fileStr.lastIndexOf(":"));
    var sep = require('path').sep;
    fileStr = fileStr.substring(fileStr.indexOf(sep), fileStr.lastIndexOf(":")).split(sep);
    var len = fileStr.length - 2;
    var msg = new Date().toUTCString() + " : " + fileStr.splice(len, 2).join("/");
//    var msg = new Date().toUTCString() + " : " + fileStr;
    console.log(msg + " :: " + str);
}
var Debug = exports.Debug = {
    _l: function (str) {
        showInConsole(str);

    },
    _i: util.inspect,
    _li: function (string, obj, isInspect) {
        var inspect;
        if (obj) {
            inspect = isInspect ? util.inspect(obj) : obj;
        }
        showInConsole(string + " " + (inspect ? inspect : ""));
    }

};

global.Debug = Debug;

var connections = 0;

var sockjs_echo = sockjs.createServer(sockjs_opts);
var liferayURL = ''; //configuration

var socketConnectURL, socketDisconnectURL , dataSendURL , pollerDestroyURL;
Properties.read(process.cwd() + "/poller.properties", function (err, props) {
    if (err) throw err;
    liferayURL = props["liferay.url"];
    socketConnectURL = liferayURL + "/poller/socketConnect/";
    socketDisconnectURL = liferayURL + "/poller/socketDisconnect/";
    dataSendURL = liferayURL + "/poller/send/";
    pollerDestroyURL = liferayURL + "/poller/destroy/";
});

var handleData = function (message) {
    var that = this;
    try {
        var messageJSON = JSON.parse(message);
        if (messageJSON && Object.keys(messageJSON).length == 2) {
            var meta = messageJSON.meta,
                send = messageJSON.send;
            var id = send.id;
            var userId = meta.userId;
            var isSignedIn = meta.isSignedIn;
            Debug._li("", messageJSON, true);

            that._userId = userId;

            if (send.action && send.action == "init") {
                var url = socketConnectURL + "?id=" + id + "&data=" + message;
                Debug._l("start ping to LR: " + url);
                request(url, function (error, response, body) {
                    if (error) {
                        Debug._l(error);
                        //possibility LR is not reachable, close all active connections
                        return Connections.shutdown();
                    }

                    Debug._l("ping to LR: " + body);
                    body = JSON.parse(body);
                    if (!error && response.statusCode == 200 && body.result == true) {
                        Connections.init(id, userId, that.id);
                        Connections.add(id, userId, that);
                    }
                });
            } else if (send.action && send.action == "send") {
                var url = dataSendURL + "?id=" + id + "&data=" + message + "&userId=" + userId;
                Debug._l("send LR: " + url);
                request(url, function (error, response, body) {
                    if (error) {
                        Debug._l(error);
                    }
                    Debug._l("ping to LR: " + body);
                });
            }
        }
    } catch (e) {
        Debug._l(e);
    }
};


var handleClose = function () {
    var connection = this,
        connectionId = connection.id;
    var id = Connections.getIdFromConnectionId(connectionId);
    if (id) {
        Connections.remove(connectionId, id);
        var userId = connection._userId;
        if (userId && Connections.activeConnectionsCountByUserId(userId) == 0) {
            request(socketDisconnectURL + "?id=" + id + "&userId=" + connection._userId, function (error, response, body) {
                if (!error && response.statusCode == 200) {
                    console.log("ping to LR: " + body);
                }
            });
        }
    }
};


sockjs_echo.on('connection', function (con) {
    ++connections;
    con.on('data', handleData);
    console.log("no of connections: " + connections);
    con.on('close', handleClose);
});

// 2. Express server
var app = express();
/* express.createServer will not work here */
var server = http.createServer(app);

sockjs_echo.installHandlers(server, {prefix: '/echo'});


app.use(express.static(__dirname + '/public'));


function doReceive(query, res) {
    var data = query.data;

    var id = query.portletId;
    var userIds = query.userIds;

    console.log(query);

    var ret = "false";
    try {
        Debug._l(data);
        if (userIds && id) {
            userIds = userIds.split(",");
            Connections.writeDataInActiveConnectionsByUserIds(userIds, id, data);
        }
        else if (id) {
            Connections.writeDataInActiveConnection(id, data);
            ret = "true";
        }

    } catch (e) {
        Debug._l(e);
    }
    res.send(ret);
}

app.get('/', function (req, res) {
    var receive = req.query.receive;
    receive = JSON.parse(receive);
    Object.keys(receive).forEach(function (key) {
        var query = receive[key];
        Debug._li("", query, true);
        doReceive(query, res);
    });
});

console.log(' [*] Listening on 0.0.0.0:9999');
server.listen(9999, '0.0.0.0');

function destroyPollerProcessor() {
    request(pollerDestroyURL, function (error, response, body) {
        process.exit(1);
    });
}

process.on('exit', function () {
    console.log('About to exit.');
    destroyPollerProcessor();
});


process.on('uncaughtException', function (err) {
    setTimeout(function () {
        Debug._l('Caught exception: ' + err);
        Debug._l(err);
        destroyPollerProcessor();
    }, 500);
});
