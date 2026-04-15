/**
 * 알림 시스템 - 헤더 드롭다운용
 * SSE 연결 및 실시간 알림 수신
 */

class NotificationManager {
    constructor() {
        this.eventSource = null;
        this.notifications = [];
        this.cursor = null;
        this.hasNext = true;
        this.isLoading = false;
        this.isDropdownOpen = false;

        this.init();
    }

    /**
     * 초기화
     */
    init() {
        this.setupElements();
        this.setupEventListeners();
        this.connectSSE();
        this.loadUnreadCount();
        this.setupInfiniteScroll();
    }

    /**
     * DOM 요소 설정
     */
    setupElements() {
        this.bellButton = document.getElementById('notificationBell');
        this.badge = document.getElementById('notificationBadge');
        this.dropdown = document.getElementById('notificationDropdown');
        this.list = document.getElementById('notificationList');
        this.markAllBtn = document.getElementById('markAllRead');
        this.sentinel = document.getElementById('notificationSentinel');
    }

    /**
     * 이벤트 리스너 설정
     */
    setupEventListeners() {
        // 벨 클릭 - 드롭다운 토글
        this.bellButton.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggleDropdown();
        });

        // 모두 읽음 버튼
        this.markAllBtn.addEventListener('click', () => {
            this.markAllAsRead();
        });

        // 외부 클릭 시 드롭다운 닫기
        document.addEventListener('click', (e) => {
            if (!this.dropdown.contains(e.target) && !this.bellButton.contains(e.target)) {
                this.closeDropdown();
            }
        });

        // 브라우저 알림 권한 요청
        this.requestNotificationPermission();
    }

    /**
     * SSE 연결
     */
    connectSSE() {
        // 세션에서 자동으로 userId 가져옴 (@LoginUser)
        this.eventSource = new EventSource(`/api/v1/notifications/stream`, {
            withCredentials: true
        });

        // 연결 성공
        this.eventSource.addEventListener('connect', (event) => {
            console.log('SSE 연결 성공:', event.data);
        });

        // 알림 수신
        this.eventSource.addEventListener('notification', (event) => {
            const notification = JSON.parse(event.data);
            this.handleNewNotification(notification);
        });

        // Heartbeat
        this.eventSource.addEventListener('heartbeat', (event) => {
            console.debug('Heartbeat:', event.data);
        });

        // 에러 처리
        this.eventSource.onerror = (error) => {
            console.error('SSE 에러:', error);
            // EventSource는 자동 재연결 시도
        };

        // 페이지 떠날 때 연결 종료
        window.addEventListener('beforeunload', () => {
            this.disconnect();
        });
    }

    /**
     * 새 알림 수신 처리
     */
    handleNewNotification(notification) {
        console.log('새 알림:', notification);

        // 목록 맨 위에 추가
        this.notifications.unshift(notification);

        // 드롭다운이 열려있으면 목록 업데이트
        if (this.isDropdownOpen) {
            this.prependNotificationToList(notification);
        }

        // 뱃지 업데이트
        this.updateBadge();

        // 토스트 알림 표시
        this.showToast(notification);

        // 브라우저 알림 (권한 있으면)
        this.showBrowserNotification(notification);
    }

    /**
     * 드롭다운 토글
     */
    toggleDropdown() {
        if (this.isDropdownOpen) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    /**
     * 드롭다운 열기
     */
    openDropdown() {
        this.dropdown.style.display = 'block';
        this.isDropdownOpen = true;

        // 처음 열 때만 로드
        if (this.notifications.length === 0) {
            this.loadNotifications();
        }
    }

    /**
     * 드롭다운 닫기
     */
    closeDropdown() {
        this.dropdown.style.display = 'none';
        this.isDropdownOpen = false;
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

            params.append('isRead', 'false');

            if (this.cursor) {
                params.append('cursor', this.cursor);
            }

            const response = await fetch(`/api/v1/notifications?${params}`, {
                headers: {
                    'Content-Type': 'application/json'
                },
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
        } catch (error) {
            console.error('알림 로드 실패:', error);
            this.showError('알림을 불러오는데 실패했습니다.');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * 알림 렌더링
     */
    renderNotifications(notifications) {
        if (notifications.length === 0 && this.notifications.length === 0) {
            this.showEmpty();
            return;
        }

        notifications.forEach(notification => {
            this.appendNotificationToList(notification);
        });
    }

    /**
     * 알림 아이템 HTML 생성
     */
    createNotificationHTML(notification) {
        const isUnread = !notification.isRead;
        const icon = this.getNotificationIcon(notification.type);
        const time = this.formatTime(notification.createdDate);

        return `
            <div class="notification-item ${isUnread ? 'unread' : ''}"
                 data-id="${notification.id}"
                 onclick="notificationManager.handleNotificationClick(${notification.id}, '${notification.referenceType}', ${notification.referenceId})">
                <div class="notification-content">
                    <div class="notification-icon">
                        <i class="${icon}"></i>
                    </div>
                    <div class="notification-body">
                        <div class="notification-text">${this.escapeHtml(notification.content)}</div>
                        <div class="notification-time">${time}</div>
                    </div>
                </div>
            </div>
        `;
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
            const fixed = dateString.replace(' ', 'T'); // 핵심
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
     * 목록 맨 위에 알림 추가 (실시간 수신)
     */
    prependNotificationToList(notification) {
        const html = this.createNotificationHTML(notification);
        this.list.insertAdjacentHTML('afterbegin', html);
    }

    /**
     * 목록 맨 아래에 알림 추가 (페이징)
     */
    appendNotificationToList(notification) {
        const html = this.createNotificationHTML(notification);
        this.list.insertAdjacentHTML('beforeend', html);
    }

    /**
     * 알림 클릭 처리
     */
    async handleNotificationClick(notificationId, referenceType, referenceId) {
        // 읽음 처리
        await this.markAsRead(notificationId);

        // 해당 페이지로 이동
        if (referenceType === 'POST') {
            // Comment ID로 게시글 찾기 (API 추가 필요) 또는 referenceId를 postId로 사용
            window.location.href = `/post/read/${referenceId}`;
        }

        this.closeDropdown();
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
                // UI 업데이트
                const item = this.list.querySelector(`[data-id="${notificationId}"]`);
                if (item) {
                    item.classList.remove('unread');
                }

                // 로컬 데이터 업데이트
                const notification = this.notifications.find(n => n.id === notificationId);
                if (notification) {
                    notification.isRead = true;
                }

                this.updateBadge();
            }
        } catch (error) {
            console.error('읽음 처리 실패:', error);
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    async markAllAsRead() {
        try {
            const response = await fetch('/api/v1/notifications/read-all', {
                method: 'PATCH',
                credentials: 'include'
            });

            if (response.ok) {
                // 모든 알림 읽음 상태로 변경
                this.list.querySelectorAll('.notification-item.unread')
                    .forEach(item => item.classList.remove('unread'));

                this.notifications.forEach(n => n.isRead = true);
                this.updateBadge();
            }
        } catch (error) {
            console.error('전체 읽음 처리 실패:', error);
        }
    }

    /**
     * 읽지 않은 알림 개수 로드
     */
    async loadUnreadCount() {
        try {
            const response = await fetch('/api/v1/notifications/unread-count', {
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                this.updateBadgeCount(data.count);
            }
        } catch (error) {
            console.error('미읽음 개수 로드 실패:', error);
        }
    }

    /**
     * 뱃지 업데이트
     */
    updateBadge() {
        const unreadCount = this.notifications.filter(n => !n.isRead).length;
        this.updateBadgeCount(unreadCount);
    }

    /**
     * 뱃지 개수 업데이트
     */
    updateBadgeCount(count) {
        if (count > 0) {
            this.badge.textContent = count > 99 ? '99+' : count;
            this.badge.style.display = 'block';
        } else {
            this.badge.style.display = 'none';
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
        }, { threshold: 1.0 });

        if (this.sentinel) {
            observer.observe(this.sentinel);
        }
    }

    /**
     * 토스트 알림 표시
     */
    showToast(notification) {
        // 간단한 토스트 (Bootstrap Toast 사용 가능)
        // 여기서는 임시로 alert 대신 콘솔만
        console.log('Toast:', notification.content);

        // TODO: 실제 토스트 UI 구현
        // 예: Bootstrap Toast, SweetAlert2 등 활용
    }

    /**
     * 브라우저 알림
     */
    showBrowserNotification(notification) {
        if (Notification.permission === 'granted') {
            new Notification('FreeCruiting', {
                body: notification.content,
                icon: '/images/logo.png',
                badge: '/images/badge.png'
            });
        }
    }

    /**
     * 브라우저 알림 권한 요청
     */
    requestNotificationPermission() {
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }

    /**
     * 로딩 표시
     */
    showLoading() {
        this.list.innerHTML = '<div class="notification-loading"><div class="spinner-border spinner-border-sm" role="status"><span class="sr-only">Loading...</span></div></div>';
    }

    /**
     * 로딩 숨김
     */
    hideLoading() {
        const loading = this.list.querySelector('.notification-loading');
        if (loading) {
            loading.remove();
        }
    }

    /**
     * 빈 목록 표시
     */
    showEmpty() {
        this.list.innerHTML = `
            <div class="notification-empty">
                <i class="fas fa-bell-slash"></i>
                <p>알림이 없습니다</p>
            </div>
        `;
    }

    /**
     * 에러 표시
     */
    showError(message) {
        this.list.innerHTML = `
            <div class="notification-empty">
                <i class="fas fa-exclamation-circle"></i>
                <p>${message}</p>
            </div>
        `;
    }

    /**
     * HTML 이스케이프
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * SSE 연결 종료
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            console.log('SSE 연결 종료');
        }
    }
}

// 전역 인스턴스 생성
let notificationManager;

document.addEventListener('DOMContentLoaded', () => {
    notificationManager = new NotificationManager();
});