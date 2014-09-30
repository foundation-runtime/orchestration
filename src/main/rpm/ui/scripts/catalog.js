
var BORDER_NON_REQUIRED = ""; // reset to default border //"1px solid #adabd3";
var BORDER_REQUIRED = "2px solid #FF0000";

/*
 * getProducts
 * Call server to get product list
 */
function getProducts() {
    $.ajax({
        type: "GET",
        url: baseUrl + "products",
        dataType: "json",
        cache: false,
        beforeSend: showMessage("Loading products..."),
        xhrFields: {
            withCredentials: true
        }
    }).done(function(data) {
    	onProductsReady(data);
        
    }).fail(function (data){
        console.error("Error retrieving product information");
    });
}

/**
 * comparator for sorting the returned products by their productName (case insensitive)
 *  so they will appear sorted on page.
 */
function compareProducts(p1,p2) {

    var p1Name = "";
    var p2Name = "";
    var p1Version = "";
    var p2Version = "";

    if (p1 != null) {

        if (p1.productName != null) {
            p1Name = p1.productName.toLowerCase();
        }
        p1Version = p1.productVersion;
    }

    if (p2 != null && p2.productName != null) {

        if (p2.productName != null) {
            p2Name = p2.productName.toLowerCase();
        }
        p2Version = p2.productVersion;
    }

    // first, compare by product name
    if (p1Name < p2Name)
        return -1;
    if (p1Name > p2Name)
        return 1;

    // if products name equals, compare by product version
    if (p1Version < p2Version)
        return -1;
    if (p1Version > p2Version)
        return 1;

    return 0;
}

/*
 * onProductsReady
 * Loop through products in list.
 * Show each product with its options
 */
function onProductsReady(data) {

    // sort array by the productName value
    data.sort(compareProducts);

    $.each(data, function(index, product){
    	var id = "product" + index;
        $(".listView").append('<div class="productwrapper"><div class="productlogo"></div>' +
        			'<div class ="producttext">' + 
        			'<span title="Click to create new instance" class="productname" pname="' +
                    product.productName + '" pver="' + product.productVersion + '"> ' + product.productName + ' - ' +
                    product.productVersion + '</span><div class="loadOptions">' + productOptionsToHtml(product, id) + 
                    '</div></div></div><div class="productsep">&nbsp;</div>');

        // Save json object as data.
        var optdiv = $(".listView").find("#" + id);
        optdiv.data("product", product);
    });
    
    $(".producttext").click(function(){
        var optdiv = $(this).find(".productoptions");
        if (optdiv.css('display') == 'none') {
            optdiv.show("fast");
            optdiv.parent().parent().parent().attr('class', 'productwrapperopen');
            $(this).attr('class', 'producttextopen');
            return false;
        } 
        return true;
    });

    $(".productname").click(function(){
        var optdiv = $(this).parent().find(".productoptions");
        showProductOptions(optdiv, (optdiv.css('display') == 'none'));
    });

    $(".instanceBtn").click(function() {
    	instantiateProduct($(this));
    });
    
    $(".cancelBtn").click(function() {
    	var optdiv = $(this).parent().parent();
    	showProductOptions(optdiv, false);
    });

    $(".subscriptionname").keyup(function() {
        $(this).css({"border":getRequiredFieldBorder($(this).val())});
    });

}

function showProductOptions(optdiv, show) {
    if (show) {
        optdiv.show("fast");
        optdiv.parent().parent().parent().attr('class', 'productwrapperopen');
        $(this).parent().attr('class', 'producttextopen');
        return false;
    } else { 
        optdiv.hide("fast");
        optdiv.parent().parent().parent().attr('class', 'productwrapper');
        $(this).parent().attr('class', 'producttext');
        return false;
    }
}
/*
 * productOptionsToHtml
 * HTML generation for a single product
 */
function productOptionsToHtml(product, id) {
	// Name parameter is implicit
	var html = '<div class="productoptions" style="display:none;" id = "' + id + '">';

    html += "<table>"
	$.each(product.productOptions, function(index, option) {
		html += "<tr><td>";

        // add <required> image before the field
        if (option.required) {
            html += "<img src=\"images/required.gif\" title='Field is required'/>";
        }

        html += "</td><td>" + option.label + ":</td><td>" + getInputTag(option) + "</td>";

        // add <help> image after the field
        html += "<td style='padding-left:5px'>";
        if (isObjectValid(option.additionalInfo) && ! isStringEmpty(option.additionalInfo.helpPageUrl)) {
            html += "<img src='images/help.gif' style='vertical-align:middle' title='Press for help about this parameter' onclick=showHelpPage('" + option.additionalInfo.helpPageUrl + "') />";
        }

        html += "</td></tr>";
    });

    html += "</table>"

	html += getInstantiationButtons(id) + "</div>";
	return html;
}

/*
 * getInputTag
 */
function getInputTag(option) {
	var html = "";
	// key, defaultValue
	switch (option.optionType.toLowerCase()) {
	case "string": 
		
		if (typeof(option.enumeration) != 'undefined' && option.enumeration != null) {
			html = writeSelect(option.key, option.defaultValue, option.label, option.required, option.enumeration);

		} else {
			html = "<" + writeInput("text", option.key, option.defaultValue, option.label, option.required) + "/>";
		}
		break;
		
	case "boolean": 
        html = writeSelect(option.key, option.defaultValue, option.label, option.required, ["True", "False"]);
        break;

	case "number":
		
		if (typeof(option.enumeration) != 'undefined' && option.enumeration != null) {
			html = writeSelect(option.key, option.defaultValue, option.label, option.required, option.enumeration);

		} else {
			html = "<" + writeInput("text", option.key, option.defaultValue, option.label, option.required) + "/>"
		}
		break;
		
    case "file":

        // replace the <object> by an input file
        var fileInputName = option.key + "FileSelection";

        // get values by which to filter the received files
        var filterRegExList = "";
        var filterValueList = "";
        if (isObjectValid(option.enumeration)) {

            for (var i=0; i<option.enumeration.length; i++) {
                var filterVal = option.enumeration[i];

                var filterRegEx;

                // if to filter file by specific pattern, build regular expression that searches a match with a file that ends with this pattern
                if (filterVal.indexOf("*.") == 0) {
                    filterRegEx = filterVal.replace("*.", ".*[.]") + "$";
                } else {
                    // set filterRegEx to be as filterVal, in case of filtering file by full match
                    filterRegEx = "^" + filterVal + "$";
                }

                if (filterRegExList.length == 0) {
                    filterRegExList = filterRegEx;
                    filterValueList = filterVal;
                } else {
                    filterRegExList = filterRegExList + "," + filterRegEx;
                    filterValueList = filterValueList + "," + filterVal;
                }
            }
        }

        // create hidden element to which file data will be inserted
        // IMPORTANT: set both <name> and <id> attributes of the element. <name> attribute is used when building the
        //              INPUT data for the materializer, while <id> attribute is used when setting the data of selected file
        //              to the hidden element.
        html = "<" + writeInput("hidden", option.key, "", option.label, option.required) +
                        " fileTypesFilter=\"" + filterValueList + "\"" +
                        " fileTypesFilterRegEx=\"" + filterRegExList + "\"/>";

        // check if the FileReader API exists... if not then load the Flash object
        if (typeof FileReader !== "function")  {

            // since that the Flex code looks for an actual DOM element and replaces it with a button, we need to create
            //  a dummy element on the document. This element will later on be deleted from the document after getting its HTML
            var objEl = window.document.createElement("OBJECT");
            objEl.id = fileInputName;
            objEl.style.visibility="hidden"; // hide element on screen
            document.body.appendChild(objEl);

            // pass parameters to the SWF
            var params = {};
            params.allowscriptaccess = "always"; // must be set in order that FLEX can invoke the JavaScript function
            params.wmode = "transparent"; // set transparent background to avoid white background

            // pass to Flash the target element id, to which write the encoded data. Note: parameters are separated by '&' characters
            var flashVars = {};
            flashVars.hiddenElementID = option.key;

            var attributes = {};
            attributes.align = "middle";
            attributes.hspace = "2px";

            // we use 85px by 22px, because we want the object to have the same size as of the button
            swfobject.embedSWF("swfobject/FileToDataURI.swf", fileInputName, "85px", "30px", "10", "swfobject/expressInstall.swf", flashVars, params, attributes);

            // read the outer HTML of the dummy element, after it was replaced by the Flex code, and remove it from the DOM hierarchy
            var flexObject = window.document.getElementById(fileInputName);

            html += flexObject.outerHTML + "<label id='" + flashVars.hiddenElementID + "Label" + "'/ style='width:100%'>";
            document.body.removeChild(flexObject);

        } else {
            html += "<" + writeInput("file", fileInputName, option.defaultValue, option.label, option.required) + " onchange='onReadFile(this, \"" + option.key + "\")'/>";

        }

		break;
		
	default:
		
		html = "--";
	
		break;
	}        
	return html;
}

// this is the function that is being called (also) by the swf file
function Flash_setFileData(filename, base64, hiddenElementID) {
    window.document.getElementById(hiddenElementID).value = base64;

    var fileNameEl = window.document.getElementById(hiddenElementID+"Label");
    if (fileNameEl != null && fileNameEl != 'undefined') {

        if (filename == null || filename == 'undefined') {
            filename = "";
        }

        fileNameEl.innerText = filename;
    }

}

// this function is being called by the swf file to display error messages
function Flash_showMessage(message) {
    alert(message);
}

// this function is being called (also) by the swf file in order to approve the selected file
function Flash_acceptFile(filename, hiddenElementID) {

    var hiddenElement = window.document.getElementById(hiddenElementID);
    var fileTypesFilterRegEx = hiddenElement.getAttribute("fileTypesFilterRegEx");
    if (isStringEmpty(fileTypesFilterRegEx)) {
        return true;
    }

    var filesFilterList = fileTypesFilterRegEx.split(",");
    for (var i=0; i<filesFilterList.length; i++) {
        var filterRegEx = new RegExp(filesFilterList[i]);

        // get if file matches the given filter
        if (filterRegEx.test(filename)) {
            return true;
        }
    }

    var fileTypesFilter = hiddenElement.getAttribute("fileTypesFilter").replace(",", "\n\t");
    alert("Selected file is not supported. Please select file that matches the following type(s):\n\n\t" + fileTypesFilter);

    // clear value of hidden element
    Flash_setFileData("", "", hiddenElementID);

    return false;
}

function writeInput (type, name, value, label, required, style) {

    var styleValue = "width:100%";
    if (required == true) {
        styleValue += "; border:" + getRequiredFieldBorder(value);
    }

    if (isObjectValid(style)) {
        styleValue += "; " + style;
    }

    var tagValue = "input" +
                   "  type='" + type +
                   "' name='" + name +
                   "' id='" + name +
                   "' value='" + value +
                   "' isValueRequired='" + required +
                   "' fieldLabel= '" + label +
                   "' style='" + styleValue + "'";

    if (required == true) {
        tagValue += " title='Field is required' onkeyup=\"this.style.border=getRequiredFieldBorder(this.value);\"";
    }

    return tagValue;
}

function writeSelect (name, value, label, required, options) {

    var style = "width:100%";
    if (required == true) {
        style += "; border:" + getRequiredFieldBorder(value);
    }

    var tagValue =  "<select" +
                "  name='"+ name +
                "' id='" + name +
                "' value='" + value +
                "' isValueRequired='" + required +
                "' fieldLabel= '" + label +
                "' style='" + style + "'";

    if (required == true) {
        tagValue += " title='Field is required' onchange=\"this.style.border=getRequiredFieldBorder(this.value);\"";
    }
    tagValue += ">";

    for (var i=0; i<options.length; i++) {
        tagValue += "<option value='" + options[i] + "'";

        if (options[i] == value) {
            tagValue += " selected='selected'"; // set selected option
        }

        tagValue += ">" + options[i] + "</option>";
    }

    tagValue += "</select>";

    return tagValue;
}

function onReadFile (element, hiddenElementID) {

    var files = element.files,file;

    if (!files || files.length == 0) return;
    file = files[0];

    // get if to accept specific file types
    if (! Flash_acceptFile(file.name, hiddenElementID)) {
        element.value = ""; // clear selection
        return;
    }

    var fileReader = new FileReader();
    fileReader.onload = function (e) {
        // ATTENTION: to have the same result than the Flash object we need to split
        // our result to keep only the Base64 part
        var base64 = e.target.result.split(",")[1];

        // set value to hidden element
        Flash_setFileData(file.name, base64, hiddenElementID);
    };
    fileReader.readAsDataURL(file);

}

function getRequiredFieldBorder (value) {

    if (isStringEmpty(value)) {
        return BORDER_REQUIRED;
    } else {
        return BORDER_NON_REQUIRED;
    }
}

function isStringEmpty(str) {
    return (!isObjectValid(str) || /^\s*$/.test(str));
}

function isObjectValid (obj) {
    return obj != null && obj != "undefined";
}

/**
 * show Help page for the given parameter
 * @param url
 */
function showHelpPage (url) {

    // open new window for the help page. make position to the left of the current element
    var _x = 0;
    var el = event.srcElement;

    while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
        _x += el.offsetLeft - el.scrollLeft;
        el = el.offsetParent;
    }

    var newWin = window.open(url, "helpWindow", "toolbar=no, scrollbars=yes, top=100, left=" + _x + ", resizable=yes, width=850, height=850")
    newWin.focus();
}

/*
 * getInstantiationButtons
 */
function getInstantiationButtons(id) {

    return "<div class='buttonswrapper'>" +
        "Instance Name: <input class='subscriptionname' autofocus='autofocus' type='text' value='' title='Field is required' style='border:" + BORDER_REQUIRED + "'/>&nbsp;&nbsp;" +
        "<input class='instanceBtn' type='button' value='Create Instance' onclick=''/>" +
        "<input class='cancelBtn' tabindex='6' type='button' value='Cancel' onclick=''/>" +
        "</div>";}

var div;
/*
 * instantiateProduct
 * Get selected values and call server to instantiate a product
 */
function instantiateProduct(button){
	var optdiv = button.parent().parent().parent().parent().find(".productoptions");

    var titlediv = optdiv.find('.subscriptionname');
    var title = titlediv.val().trim();

    if(title == ""){
        alert("Missing required field: \"Instance Name\"");
        titlediv.focus();
        return false;
    }
    var product = optdiv.data("product");
    
    div = optdiv;
    var dataIsValid = true;

    // Update values in product
    $.each(product.productOptions, function(index, opt) {
        var elements = div.find("[name='" + this.key + "']");
        if (elements.size() > 0) {
            var element = elements.get(0);

            var val = element.value;
            var isValueRequired = element.getAttribute("isValueRequired");

            if (isObjectValid(isValueRequired) && isValueRequired == "true" && isStringEmpty(val)) {
                alert("Missing required field: \"" + element.getAttribute("fieldLabel") + "\"");
                element.focus();
                dataIsValid = false;
                return false;
            }
            //
            this.value = val;
        }
    });

    if (! dataIsValid) {
        return false;
    }

    var instanceData = {
        "instanceId":null, 
        "systemId": systemCookie, 
        "instanceName": title, 
        "status": "initiating",
        "product": product
    };
    console.log(JSON.stringify(instanceData));
    $.ajax({
        type: "POST",
        url: baseUrl + "products/" + product.productName + "-" + product.productVersion + "/instantiate",
        processData: false,
        beforeSend: showMessage("Creating new instance..."),
		dataType: "json",
        contentType: 'application/json',
        data: JSON.stringify(instanceData),
		xhrFields: {
			withCredentials: true
		}
    }).done(function(data) {
		//console.log(data);
		sessionStorage.setItem("instanceId", data.instanceId);
		$(".loadOptions").empty();
		window.location.replace("instances.html");
		$("#msgBar").html("New instance created").delay(5000).fadeOut();
	}).fail(function (data){
		console.error("Error creating instances");
	});
    
}
