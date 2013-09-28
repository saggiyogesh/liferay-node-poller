var util = require("util");

var connections = {};
var idConnectionIdMapping = {};
var connectionId_Id = {};
var userIdConnectionId = {};

exports.init = function (id, userId, connectionId) {
    if (!idConnectionIdMapping.hasOwnProperty(id)) {
        connections[connectionId] = [];
        idConnectionIdMapping[id] = [];
        connectionId_Id[connectionId] = id;
    }
    else {
        Debug._l("Already init: " + id);
    }

    if (!userIdConnectionId.hasOwnProperty(userId)) {
        userIdConnectionId[userId] = [];
    }
    else {
        Debug._l("User already init: " + userId);
    }
};

exports.add = function (id, userId, connection) {
    var connectionId = connection.id;
    idConnectionIdMapping[id].push(connectionId);
    connections[connectionId] = connection;
    connectionId_Id[connectionId] = id;
    userIdConnectionId[userId].push(connectionId);
};

exports.remove = function (connectionId, id) {
    id = id || getIdFromConnectionId(connectionId);
    if (id) {
        var connectionIdArr = idConnectionIdMapping[id];
        var idx = connectionIdArr.indexOf(connectionId);
        if (idx > -1) {
            connectionIdArr.splice(idx, 1);
        }
    }

    deleteUserConnection(connectionId);
    delete connectionId_Id[connectionId];
    delete connections[connectionId];
};

function deleteUserConnection(connectionId) {
    var userIds = Object.keys(userIdConnectionId);
    for (var i = 0; i < userIds.length; i++) {
        var connIds = userIdConnectionId[userIds[i]];
        var idx = connIds.indexOf(connectionId);
        if (idx > -1) {
            connIds.splice(idx, 1);
            break;
        }
    }
}

function getUserIdFromConnectionId(connectionId) {
    var userIds = Object.keys(userIdConnectionId);
    for (var i = 0; i < userIds.length; i++) {
        var connIds = userIdConnectionId[userIds[i]];
        var idx = connIds.indexOf(connectionId);
        if (idx > -1) {
            return userIds[i];
        }
    }
};

var getIdFromConnectionId = exports.getIdFromConnectionId = function (connectionId) {
    return connectionId_Id[connectionId];
};

var activeConnections = exports.activeConnections = function (id, returnConnectionsArray) {
    if (returnConnectionsArray) {
        var connectionIdArr = idConnectionIdMapping[id];
        var arr = [];
        connectionIdArr.forEach(function (conId) {
            arr.push(connections[conId]);
        });
        return arr;
    }
    return idConnectionIdMapping[id] || [];
};

exports.writeDataInActiveConnection = function (id, data) {
    var connectionIdArr = idConnectionIdMapping[id];
    connectionIdArr && connectionIdArr.forEach(function (conId) {
        connections[conId].write(data);
    });
};

exports.writeDataInActiveConnectionsByUserIds = function (userIds, id, data) {
    userIds = util.isArray(userIds) ? userIds : [userIds];
    userIds && userIds.forEach(function (userId) {
        var conIds = userIdConnectionId[userId];
        conIds && conIds.forEach(function (conId) {
            if (connectionId_Id[conId] == id) { //write to those connections which are related to both userId and id
                connections[conId].write(data);
            }
        });
    })
};

exports.activeConnectionsCountByUserId = function (userId) {
    return userIdConnectionId[userId].length || 0
};
exports.activeConnectionsCount = function (id) {
    return activeConnections(id).length;
};

exports.shutdown = function () {
    connections.forEach(function (connection) {
        delete connection._userId;
        connection.close();
    });
};

/*var timers = require("timers");
 timers.setInterval(function () {
 Debug._l(Object.keys(userIdConnectionId) + " :: "+Object.keys(connections).length + " :: " + Object.keys(connectionId_Id).length)
 Debug._li(":" , userIdConnectionId, true);
 }, 5000)*/
