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

        $('#btn-update-user').on('click', function () {
            _this.updateUser();
        });

        $('#btn-save-comment').on('click', function () {
            _this.saveComment();
        });

        $('#btn-update-comment').on('click', function () {
            _this.updateComment();
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

        if(data.title === '') {
            data.title = '빈 제목';
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
    },

    updateUser : function () {
        var data = {
            name: $('#name').val(),
            picture: $('#picture').val(),
        };

        $.ajax({
            type: 'PUT',
            url: '/api/v1/user',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function(response) {
            alert(response.message);
            window.location.href = '/';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
    },
};
// Comment 함수는 이렇게 하는  것이 더 적절한 듯
function saveComment(post_id) {
        var data = {
            post_id: post_id,
            content: $('#comment-content-save').val()
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/comment',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Register New Comment.');
            location.reload();
            //window.location.href = `/post/read/${data.post_id}`;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function updateComment() {
        var comment_id = $('#comment-id-update').val()

        var data = {
            content: $('#comment-content-update').val()
        };

        $.ajax({
            type: 'PUT',
            url: '/api/v1/comment/' + comment_id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Update Comment.');
            location.reload();
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}



function deleteComment(comment_id) {
    if (!confirm("Are you sure you want to delete this comment?")) return;

    $.ajax({
        type: 'DELETE',
        url: '/api/v1/comment/' + comment_id, // 댓글 ID를 URL에 추가
        dataType: 'json',
        contentType: 'application/json; charset=utf-8'
    }).done(function() {
        alert('Comment deleted successfully.');
        location.reload(); // 페이지 새로고침
    }).fail(function(error) {
        alert("Error deleting comment: " + JSON.stringify(error));
    });
}


main.init()