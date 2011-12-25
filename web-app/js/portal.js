function isEmpty(str) {
    return (!str || 0 === str.length || /^\s*$/.test(str));
}

function checkLogin() {
    var username = $('#username').val();
    var password = $('#passwd').val();
    return !isEmpty(username) && !isEmpty(password);
}

function toggleWatcher(data, watchingId) {
    var src = $('#' + watchingId).attr('src');
    src = src.substring(0, src.lastIndexOf('/'));
    if (data.status) {
        src += '/watch_on.png';
    } else {
        src += '/watch_off.png';
    }
    $('#' + watchingId).attr('src', src);
}

function handleTagsResponse(data) {
    if (data.code == 'ERROR') {
        $('#tags-message').html('<p>An error occurred processing your request. Please try again.</p>');
        $('#tags-message').show();
    } else if (data.code == 'OK') {
        $('#tags-message').hide();
        $('#tags-submit').hide();
        $('#tags-cancel').toggleClass('danger success', true);
        $('#tags-apply').toggleClass('primary info', true);
        $('#tags-apply').attr('disabled', true);
        $('#artifact-tags').html(data.tags);
    }
}