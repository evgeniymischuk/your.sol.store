$(document).ready(function () {
    var els = $('.downloaded-item');
    count = 0;
    $.each(els, function (i, el) {
        loadImage($(el));
    });
    setTimeout(function () {
        sendRequest($(arrRequest[0]))
    }, 0);
});
var arrRequest = [];
var count;

function loadImage(el) {
    if (el.data('load') !== true) {
        if (el.data('dir') !== undefined && el.attr('id') !== undefined) {
            arrRequest.push(el);
        }
    }
}

function sendRequest(el) {
    count++;
    console.log('loadpic');
    var name = el.attr('id');
    var lowerCaseName = name.toLowerCase();
    el.removeClass("d-none");
    var others = false;
    var min = count < 3;
    if (lowerCaseName.includes(".jpg") || lowerCaseName.includes(".jpeg")) {
        var button = $(document.getElementById(name + "DownloadButton"));
        var url = "../cloud/download?dir=" + encodeURIComponent(el.data('dir')) + "-web" + "&name=" + encodeURIComponent(name);
        if (min){
            el.removeClass("animation-show");
        }
        el.attr('style', 'background: url(\'' + url + '\');opacity: 1;display:block; ');
        el.data('load', true);
        setTimeout(function () {
            button.removeClass("d-none");
        }, min ? 0 : 1000);

    } else if (lowerCaseName.includes(".mov") || lowerCaseName.includes(".mp4")) {
        // var button = $(document.getElementById(name+"DownloadButton"));
        var embed = $(document.getElementById(name + "embed"));
        var url = "../cloud/download?dir=" + encodeURIComponent(el.data('dir')) + "&name=" + encodeURIComponent(name);
        el.attr('src', url);
        var video = document.getElementById(name + "Video");
        video.load();
        $(embed).attr("style", "opacity:1;");
        el.data('load', true);
        // setTimeout(function () {
        //     button.removeClass("d-none");
        // }, 1500);
    } else {
        var url = "../cloud/download?dir=" + encodeURIComponent(el.data('dir')) + "&name=" + encodeURIComponent(name);
        el.html(name);
        el.attr('href', url);
        el.data('load', true);
        others = true;
    }

    if (count < arrRequest.length) {
        setTimeout(function () {
            sendRequest(arrRequest[count])
        }, min || others ? 0 : 500);
    }
}
