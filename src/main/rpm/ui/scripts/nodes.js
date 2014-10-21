function showInstanceModules() {
var n = window.location.href.lastIndexOf("=");
var instanceId = window.location.href.substring(n + 1);
$.ajax({
    type: "GET",
    url: baseUrl + "systems/" + systemCookie + "/instances/" + instanceId,
    dataType: "json",
    beforeSend: showMessage("Loading instance information..."),
    cache: false,
    xhrFields: {
        withCredentials: true
    }
}).done(function (data) {
    var mlist = "<table border=1>";
    $(".instancetitle").html("Instance:" + data.instanceName + ". System: " + data.systemId);
    $.each(data.machineIds, function (index, mi) {
            mlist += "<tr><td>" + mi.hostname + "</td><td>";
            $.each(mi.installedModules, function (index, m) {
                mlist += m + "<br/>";
            });
            mlist += "</td></tr>";
        });
        mlist += "</table>";
    $(".instancedata").html("<div class='detailsdata'>" + mlist + "</div>");
}).fail(function (data) {
    console.error("Error retrieving instance details");
});
}

