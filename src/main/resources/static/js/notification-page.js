/**
 * 알림 페이지 (/notifications)
 * 전체 알림 목록 및 필터링
 */

class NotificationPageManager {
    constructor() {
        this.notifications = [];
        this.cursor = null;
        this.hasNext = true;
        this.isLoading = false;
        this.currentFilter = 'all'; // all, unread

        this.init();
    }

    init() {
        this.setupElements();
        this.setupEventListeners();
        this.loadNotifications();
        this.setupInfiniteScroll();
    }

    setupElements() {
        this.listContainer = document.getElementById('notificationPageList');
        this.loading = document.getElementById('pageLoading');
        this.sentinel = document.getElementById('pageSentinel');
        this.noNotifications = document.getElementById('noNotifications');
        this.markAllBtn = document.getElementById('markAllReadPage');
        this.filterButtons = document.querySelectorAll('[data-filter]');
    }

    setupEventListeners() {
        // 필터 버튼
        this.filterButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.handleFilterChange(e.target.dataset.filter);
            });
        });

        // 모두 읽음
        if (this.markAllBtn) {
            this.markAllBtn.addEventListener('click', () => {
                this.markAllAsRead();
            });
        }
    }

    /**
     * 필터 변경
     */
    handleFilterChange(filter) {
        if (this.currentFilter === filter) return;

        this.currentFilter = filter;

        // 버튼 활성화 상태 변경
        this.filterButtons.forEach(btn => {
            if (btn.dataset.filter === filter) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // 목록 초기화 및 재로드
        this.reset();
        this.loadNotifications();
    }

    /**
     * 알림 목록 로드
     */
    async loadNotifications() {
        if (this.isLoading || !this.hasNext) return;

        this.isLoading = true;
        this.showLoading();

        try {
            const params = new URLSearchParams({
                size: 20
            });

            if (this.cursor) {
                params.append('cursor', this.cursor);
            }

            if (this.currentFilter === 'unread') {
                params.append('isRead', 'false');
            }

            const response = await fetch(`/api/v1/notifications?${params}`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('알림 로드 실패');
            }

            const data = await response.json();

            this.notifications.push(...data.notifications);
            this.cursor = data.nextCursor;
            this.hasNext = data.hasNext;

            this.renderNotifications(data.notifications);

            // 알림 없으면 빈 상태 표시
            if (this.notifications.length === 0) {
                this.showEmpty();
            } else {
                this.hideEmpty();
            }

        } catch (error) {
            console.error('알림 로드 실패:', error);
            alert('알림을 불러오는데 실패했습니다.');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * 알림 렌더링
     */
    renderNotifications(notifications) {
        notifications.forEach(notification => {
            this.appendNotification(notification);
        });
    }

    /**
     * 알림 아이템 추가
     */
    appendNotification(notification) {
        const html = this.createNotificationHTML(notification);
        this.listContainer.insertAdjacentHTML('beforeend', html);
    }

    /**
     * 알림 HTML 생성
     */
    createNotificationHTML(notification) {
        const isUnread = !notification.isRead;
        const icon = this.getNotificationIcon(notification.type);
        const time = this.formatTime(notification.createdAt);

        return `
            <div class="notification-page-item ${isUnread ? 'unread' : ''}"
                 data-id="${notification.id}"
                 onclick="notificationPageManager.handleNotificationClick(${notification.id}, '${notification.referenceType}', ${notification.referenceId})">
                <div class="notification-page-content">
                    <div class="notification-page-icon">
                        <i class="${icon}"></i>
                    </div>
                    <div class="notification-page-body">
                        <div class="notification-page-text">${this.escapeHtml(notification.content)}</div>
                        <div class="notification-page-time">${time}</div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 알림 클릭 처리
     */
    async handleNotificationClick(notificationId, referenceType, referenceId) {
        // 읽음 처리
        await this.markAsRead(notificationId);

        // 페이지 이동
        if (referenceType === 'POST') {
            window.location.href = `/post/read/${referenceId}`;
        }
    }

    /**
     * 특정 알림 읽음 처리
     */
    async markAsRead(notificationId) {
        try {
            const response = await fetch(`/api/v1/notifications/${notificationId}/read`, {
                method: 'PATCH',
                credentials: 'include'
            });

            if (response.ok) {
                const item = this.listContainer.querySelector(`[data-id="${notificationId}"]`);
                if (item) {
                    item.classList.remove('unread');
                }

                const notification = this.notifications.find(n => n.id === notificationId);
                if (notification) {
                    notification.isRead = true;
                }
            }
        } catch (error) {
            console.error('읽음 처리 실패:', error);
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    async markAllAsRead() {
        if (!confirm('모든 알림을 읽음 처리하시겠습니까?')) {
            return;
        }

        try {
            const response = await fetch('/api/v1/notifications/read-all', {
                method: 'PATCH',
                credentials: 'include'
            });

            if (response.ok) {
                // 모든 unread 클래스 제거
                this.listContainer.querySelectorAll('.notification-page-item.unread')
                    .forEach(item => item.classList.remove('unread'));

                this.notifications.forEach(n => n.isRead = true);

                alert('모든 알림을 읽음 처리했습니다.');
            }
        } catch (error) {
            console.error('전체 읽음 처리 실패:', error);
            alert('읽음 처리에 실패했습니다.');
        }
    }

    /**
     * 무한 스크롤 설정
     */
    setupInfiniteScroll() {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && this.hasNext && !this.isLoading) {
                this.loadNotifications();
            }
        }, { threshold: 0.5 });

        if (this.sentinel) {
            observer.observe(this.sentinel);
        }
    }

    /**
     * 초기화
     */
    reset() {
        this.notifications = [];
        this.cursor = null;
        this.hasNext = true;
        this.listContainer.innerHTML = '';
    }

    /**
     * 로딩 표시
     */
    showLoading() {
        if (this.loading) {
            this.loading.style.display = 'block';
        }
    }

    /**
     * 로딩 숨김
     */
    hideLoading() {
        if (this.loading) {
            this.loading.style.display = 'none';
        }
    }

    /**
     * 빈 상태 표시
     */
    showEmpty() {
        if (this.noNotifications) {
            this.noNotifications.style.display = 'block';
        }
    }

    /**
     * 빈 상태 숨김
     */
    hideEmpty() {
        if (this.noNotifications) {
            this.noNotifications.style.display = 'none';
        }
    }

    /**
     * 알림 타입별 아이콘
     */
    getNotificationIcon(type) {
        const icons = {
            'POST_COMMENT': 'fas fa-comment',
            'COMMENT_REPLY': 'fas fa-reply',
            'COMMENT_MENTION': 'fas fa-at'
        };
        return icons[type] || 'fas fa-bell';
    }

    /**
     * 시간 포맷팅
     */
    formatTime(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diff = Math.floor((now - date) / 1000);

        if (diff < 60) return '방금 전';
        if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
        if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
        if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`;

        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }

    /**
     * HTML 이스케이프
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 전역 인스턴스
let notificationPageManager;

document.addEventListener('DOMContentLoaded', () => {
    notificationPageManager = new NotificationPageManager();
});