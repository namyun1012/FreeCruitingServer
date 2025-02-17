// 추후 React 에서 써먹기

var main = {
    init : function () {
        var _this = this;
        $('#btn-save').on('click', function () {
            _this.save();
        });

        $('#btn-update').on('click', function () {
            _this.update();
        });

        $('#btn-delete').on('click', function () {
            _this.delete();
        });
    },
    save : function () {
        var data = {
            title: $('#title').val(),
            author: $('#author').val(),
            type: $('#type').val(),
            imageURL: 'imageURL',
            content: $('#content').val()
        };

        if(!title) {
            title = '빈 제목';
        }


        $.ajax({
            type: 'POST',
            url: '/api/v1/post',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Register New Post.');
            window.location.href = '/';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
    },
    update : function () {
        var data = {
            title: $('#title').val(),
            type: $('#type').val(),
            imageURL: 'imageURL',
            content: $('#content').val()
        };

        var id = $('#id').val();

        $.ajax({
            type: 'PUT',
            url: '/api/v1/post/'+id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function(response) {
            alert(response.message);
            window.location.href = '/';
        }).fail(function (error) {
            if(error.status === 403) {
                alert(error.responseJSON.error);
            } else {
                alert(JSON.stringify(error));
            }
        });
    },
    delete : function () {
        var id = $('#id').val();

        $.ajax({
            type: 'DELETE',
            url: '/api/v1/post/'+id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8'
        }).done(function(response) {
            alert(response.message);
            window.location.href = '/';
        }).fail(function (error) {
            if(error.status === 403) {
                alert(error.responseJSON.error);
            } else {
                alert(JSON.stringify(error));
            }
        });
    }

};

main.init()