function showInstanceDetails() {
}
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
    var details = "";
    $(".instancetitle").html("Instance:" + data.instanceName + ". System: " + data.systemId);
    $.each(data.machineIds, function (index, ap) {
            details += "<div>" +
                ap.hostname +
                "<div>" +
                ap.installedModules
            "</div>" +
            "</div>"
        }
    )
    $(".instancedata").html("<div class='detailsdata'>" + data.status + "<br/>" + details + "</div>");
}).fail(function (data) {
    console.error("Error retrieving instance details");
});


