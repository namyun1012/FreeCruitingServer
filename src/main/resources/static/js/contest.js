/* contest.js — Contest domain */
var ContestApp = (function ($) {
    'use strict';

    var API_BASE = '/api/v1/contests';

    /* ── Delete (detail page) ───────────────────────────── */
    function deleteContest(id) {
        if (!confirm('정말 삭제하시겠습니까?')) return;
        $.ajax({ url: API_BASE + '/' + id, type: 'DELETE' })
            .done(function () {
                alert('삭제되었습니다.');
                window.location.href = '/contest';
            })
            .fail(function (xhr) {
                alert('삭제 실패 (' + xhr.status + ')');
            });
    }

    /* ── List Page Init ─────────────────────────────────── */
    $(function () {
        if (!$('.category-filter-btn').length) return;

        var params   = new URLSearchParams(window.location.search);
        var category = params.get('category') || '';
        var keyword  = params.get('keyword')  || '';

        /* 현재 카테고리 버튼 active 표시 */
        $('.category-filter-btn').each(function () {
            if ($(this).data('category') === category) {
                $(this).removeClass('btn-outline-secondary').addClass('btn-primary');
            }
        });

        /* pages.mustache 페이지네이션 링크에 category/keyword 파라미터 보정 */
        $('.pagination .page-link').each(function () {
            var href = $(this).attr('href');
            if (!href || !href.startsWith('?page=')) return;

            var pageNum = href.replace('?page=', '');
            var next = new URLSearchParams();
            next.set('page', pageNum);
            if (category) next.set('category', category);
            if (keyword)  next.set('keyword',  keyword);
            $(this).attr('href', '?' + next.toString());
        });
    });

    return { deleteContest: deleteContest };

}(jQuery));
