$(document).ready(function() {

    function getParameterByName(name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(location.search);
        return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    }
    $('.page-index').click(function () {
        var pageNumber = $(this).text();
        var q = getParameterByName('query');
        var resultsperpage = getParameterByName('results_per_page');
        var queryString = 'query='+q+"&results_per_page="+resultsperpage+"&page_number="+pageNumber;
        var relocateUrl = window.location.href.split('?')[0] + '?' +queryString;
        window.location.href = relocateUrl;

    });

    var searchRefresh = function(){
        var pageNumber = 1;
        var q = $('#solr-search-query').val();
        var resultsperpage = getParameterByName('results_per_page');
        var queryString = 'query='+q+"&results_per_page="+resultsperpage+"&page_number="+pageNumber+"&doing_solr_search=true";
        var relocateUrl = window.location.href.split('?')[0] + '?' +queryString;
        window.location.href = relocateUrl;
    }

    $('.solr-results-clear-button').click(function() {
        $('#searchResults').empty();
        $('#solr-search-query').val('');
        $('#solr-results-info').html("About 0 Results");
    });

    $('.solr-search-button').click(function(){
        searchRefresh();
    });
});





