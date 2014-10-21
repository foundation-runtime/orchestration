var noDataMSG = 'No subscription available. Please navigate to <a class="link" href="products.html">Product Screen</a> for available products.';
$(document).ready(function() {
    $(".systemName").html(systemCookie);
    getPageData();

});

function getPageData(){
    $("#listView").empty();
    $.ajax({
        type: "GET",
        url: baseUrl + "systems/" + systemCookie + "/instances",
        dataType: "json",
        cache: false,
        beforeSend: showMessage("Loading instances..."),
        xhrFields: {
            withCredentials: true
        }
    }).done(function(data) {
        //console.log(data);
        if(data.length == 0) {
            $("#listView").append('<div>'+noDataMSG+'</div>');
            return false;
        }
        $(".listView").html("");
        $.each(data, function(index, instance){
            var statusClass = "";
            var status = instance.status.toLowerCase();
            if(status == "completed" || status == "started") {
                statusClass = 'statusRunning';
                statusText = "RUNNING";
            } else if(status == "failed") {
                statusClass = 'statusFailed';
                statusText = "  ERROR";
            } else if(status == "in-progress" || status == "starting") {
                statusClass = 'statusStarting';
                statusText = "  STARTING";
            } else {
                statusText = "STOPPED";
                statusClass = 'statusStarting';
            }

            var html =
                '<div class="' + instance.instanceId + '">' +
                '<div class="productwrapper"><div class="instancelogo"></div>' +
                '<div class ="producttext">' +
                '<span title="Click to view info" class="sName" sid="' +
                instance.instanceId + '">' + instance.product.productName + ' - ' +
                instance.product.productVersion + ' (' + instance.instanceId + ') </span>' +
                '<div class="statuswrapper">' +
                '<div class="' + statusClass + 'Img">&nbsp;</div>' +
                '<div class="' + statusClass + '">' + statusText + '</div>' +
                '</div>' +
                '<div class="loadInfo"></div></div></div><div class="productsep">&nbsp;</div></div>';
            $(".listView").append(html);
        });


        $(".producttext").click(function(){
            loadinfodiv = $(this).find(".loadInfo");
            if(loadinfodiv.html() == ""){
                loadinfodiv.show("slow");
                namediv = $(this).find(".sName");
                showInstanceInfo(namediv);
                return false;
            }
            return true;
        });

        $(".sName").click(function(){
            loadinfodiv = $(this).parent().parent().find(".loadInfo");
            if(loadinfodiv.html() != ""){
                sessionStorage.removeItem("instanceId");
                loadinfodiv.hide("slow");
                loadinfodiv.empty();
                $(this).parent().attr('class', 'producttext');
                $(this).parent().parent().attr('class', 'productwrapper');
                return false;
            }
            else
                return true;
        });

        //trigger newly created instance
        if(sessionStorage.getItem("instanceId") != null){
            $('.sName[sid="' + sessionStorage.getItem("instanceId") + '"]').trigger("click");
            //sessionStorage.removeItem("instanceId");
        }

    }).fail(function (data){
        console.error("Error retrieving instance information");
    });
}

function deleteInstance(instanceId, that){
    if (!confirm("Do you want to delete instance id " + instanceId + "?")){
        return false;
    }
    $.ajax({
        type: "DELETE",
        url: baseUrl + "systems/" + systemCookie + "/instances/" + instanceId,
        dataType: "json",
        beforeSend: showMessage("Deleting instance..."),
        cache: false,
        xhrFields: {
            withCredentials: true
        }
    }).done(function(data) {
        $('.' +instanceId ).hide("fast");

    }).fail(function (data){
        console.error("Error Deleting instance details");
    });
}

function showInstanceInfo(that){
    var instdiv = $(that).parent().parent().find(".loadInfo");
    instanceId = $(that).attr("sid");
    var prodwrapdiv = $(that).parent().parent();
    //$('.productwrapperopen').attr('class', 'productwrapper');
    //$('.producttextopen').attr('class', 'producttext');
    prodwrapdiv.attr('class', 'productwrapperopen');
    $(that).parent().attr('class', 'producttextopen');
    $.ajax({
        type: "GET",
        url: baseUrl + "systems/" + systemCookie + "/instances/" + instanceId,
        dataType: "json",
        beforeSend: showMessage("Loading instance information..."),
        cache: false,
        xhrFields: {
            withCredentials: true
        }
    }).done(function(data) {
        var deletebtn="";
        if (data.deletable == null || data.deletable  == true) {
            deletebtn = '<div class="buttonswrapper"><input type="button" value="Delete" class="deleteButton" style="padding: 10px" onclick=""/></div>';
        }

        var html = '<div id="instanceDetails">' +
            '<div class="instancerow"><div class = "instanceprop">Instance Name:</div><div class="instancedata" id="instanceData"></div></div>'+
            '<div class="instancerow"><div class = "instanceprop">Entry points:</div><div class="instancedata" id="apiData">-</div></div>'+
            '<div class="instancerow"><a class="statusdetails" href="#">Status Details</a></div>' +
            '<div class="instancerow"><a class="nodesdetails" href="#">Nodes Details</a></div>' +
            '<div class="instancerow">' + deletebtn + '</div>' +
            '</div>';

        console.log(data);
        instdiv.show("fast");
        instdiv.html(html);
        instdiv.find("#instanceData").html(data.instanceName + " - (" + data.instanceId + ")");

        var status = data.status.toLowerCase();
        if(status == "completed" || status == "started") {
            var apidata = "";
            $.each(data.accessPoints, function(index, ap) {
                if (apidata.length > 0) {
                    apidata += "<br/>";
                }
                apidata += ap.name + ' - <a class="link" href="' + ap.url + '" target="_blank">' + ap.url + '</a>';
            });
            instdiv.find("#apiData").html(apidata);
        }
        instdiv.find(".deleteButton").click(function(){
            deleteInstance(data.instanceId, this);
            return false;
        });

        instdiv.find(".statusdetails").click(function(){
            window.location.replace("instance.html?id=" + data.instanceId);
            return false;
        });
        instdiv.find(".nodesdetails").click(function(){
            window.location.replace("nodes.html?id=" + data.instanceId);
            return false;
        });


    }).fail(function (data){
        console.error("Error retrieving instance details");
    });
}
