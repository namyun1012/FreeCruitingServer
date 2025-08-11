
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
            url: '/api/v1/posts',
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
            url: '/api/v1/posts/'+id,
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
            url: '/api/v1/posts/'+id,
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
        var userName = $('#name').val();
        var newPictureFile = $('#newPictureFile')[0].files[0];

        var formData = new FormData();

        formData.append('name', userName);

        // 새 이미지 선정 시에만 FormData 에 추가
        if (newPictureFile) {
            formData.append('file', newPictureFile);
        }

        $.ajax({
            type: 'PUT',
            url: '/api/v1/users',
            processData: false,
            contentType: false,
            data: formData
        }).done(function(response) {
            alert(response.message);
            window.location.href = '/';
        }).fail(function (xhr, status, error) {
            var errorMessage = xhr.responseText || '업데이트 실패: 알 수 없는 오류';
            try {
                // 서버에서 JSON 응답을 보낼 경우 파싱 시도
                var errorJson = JSON.parse(errorMessage);
                errorMessage = errorJson.message || errorMessage;
            } catch (e) {
                // JSON 파싱 실패 시 원본 텍스트 사용
            }
            alert('업데이트 실패: ' + errorMessage);
            console.error("AJAX Error:", status, error, xhr.responseText);
        });
    },
};


function saveUser() {
        var data = {
            name : $('#name').val(),
            email : $('#email').val(),
            password : $('#password').val(),
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/users',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('회원 가입 완료');
            window.location.href = '/';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}


// Comment 함수는 이렇게 하는  것이 더 적절한 듯
function saveComment(post_id) {
        var data = {
            post_id: post_id,
            content: $('#comment-content-save').val()
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/comments',
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
            url: '/api/v1/comments/' + comment_id,
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
        url: '/api/v1/comments/' + comment_id, // 댓글 ID를 URL에 추가
        dataType: 'json',
        contentType: 'application/json; charset=utf-8'
    }).done(function() {
        alert('Comment deleted successfully.');
        location.reload(); // 페이지 새로고침
    }).fail(function(error) {
        alert("Error deleting comment: " + JSON.stringify(error));
    });
}

// party 함수
function saveParty() {
        var data = {
            name: $('#party-name-save').val(),
            description: $('#party-description-save').val(),
            max_number: $('#party-max_number-save').val()
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/partys',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Register New Party.');
            window.location.href = '/party';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function updateParty() {
        var party_id = $('#party-id-update').val();

        var data = {
            name: $('#party-name-update').val(),
            description: $('#party-description-update').val(),
            max_number: $('#party-max_number-update').val()
        };

        $.ajax({
            type: 'PUT',
            url: '/api/v1/partys/' + party_id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Update Party.');
            window.location.href = '/party';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function deleteParty() {
        var party_id = $('#party-id-update').val();

        $.ajax({
            type: 'DELETE',
            url: '/api/v1/partys/' + party_id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8'
        }).done(function() {
            alert('Delete Party.');
            window.location.href = '/party';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function savePartyMember(party_id) {
        var data = {
            party_id: party_id,
            party_role: "MEMBER"
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/partymembers',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Join Party.');
            window.location.href = '/party/read/' + party_id;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function updatePartyMember() {
        var data = {
        };

        $.ajax({
            type: 'PUT',
            url: '/api/v1/partymembers',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('Update Party Role.');
            window.location.href = '/party/read/' + party_id;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function deletePartyMember(partymember_id, party_id) {

        $.ajax({
            type: 'DELETE',
            url: '/api/v1/partymembers/' + partymember_id,
            dataType: 'json',
            contentType:'application/json; charset=utf-8'
        }).done(function() {
            alert('Out Party.');
            window.location.href = '/';
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

// Party Join Request 영역

function savePartyJoinRequest(party_id) {
        var data = {
            party_id: party_id,
            party_role: "MEMBER"
        };

        $.ajax({
            type: 'POST',
            url: '/api/v1/party_join_requests',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function() {
            alert('파티 가입 요청.');
            window.location.href = '/party/read/' + party_id;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
}

function approvePartyJoinRequest(id ,party_id) {

        $.ajax({
            type: 'PUT',
            url: '/api/v1/party_join_requests/' + id + '/approve',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
        }).done(function() {
            alert('파티 가입 승인.');
            window.location.href = '/party/update/' + party_id;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });

}

function rejectPartyJoinRequest(id ,party_id) {

        $.ajax({
            type: 'PUT',
            url: '/api/v1/party_join_requests/' + id + '/reject',
            dataType: 'json',
            contentType:'application/json; charset=utf-8',
        }).done(function() {
            alert('파티 가입 거절.');
            window.location.href = '/party/update/' + party_id;
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });

}

main.init()