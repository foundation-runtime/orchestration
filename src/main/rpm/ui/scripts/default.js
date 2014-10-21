var baseUrl = "http://10.45.37.127:6401/";
var systemCookie = "izek" // localStorage.systemCookie;

$(document).ajaxStart(function() {
    //showMessage( "Loading..." );
});
/*
$(document).ajaxStop(function() {
    $("#msgBar").append( "Triggered ajaxStop handler.<br>" );
});

$( document ).ajaxComplete(function() {
    $("#msgBar").append( "Triggered ajaxComplete handler.<br>" );
});
*/
$( document ).ajaxError(function() {
    $("#msgBar").addClass("error");
    showMessage( "Error..." );
});

$(document).ajaxSuccess(function() {
    $("#msgBar").fadeOut();
    //$("#msgBar").html( "Triggered ajaxSuccess handler.<br>" );
});

function showMessage(m){
    $("#msgBar").show().html(m);
    
}

$(document).ready(function() {
	
    $("#logout").click(function(){
		localStorage.systemCookie == "";
        window.location.replace("index.html");
    });

    $("#greetingUser").html("Welcome " + systemCookie);

    $(".topNav").click(function(){
        window.location=$(this).find("a").attr("href");
        return false;
    });

    $('#loginbutton').click(function() {
//        window.location.href = 'products.html';
//        return false;
    });
});
