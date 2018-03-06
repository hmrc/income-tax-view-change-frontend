$(document).ready($(function () {

    $('[data-metrics]').each(function () {
        var metrics = $(this).attr('data-metrics');
        var parts = metrics.split(':');
        if(parts.length == 3){
            ga('send', 'event', parts[0], parts[1], parts[2]);
        } else {
            ga('send', 'event', parts[0], parts[1], parts[2], parts[3]);
        }
    });

    $('[click-metrics]').on('click', function() {
        var metrics = $(this).attr('click-metrics');
        var parts = metrics.split(':');
        ga('send', 'event', parts[0], parts[1], 'click');
    });

}));
