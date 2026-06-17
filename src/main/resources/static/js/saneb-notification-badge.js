(() => {
    const link = document.querySelector("[data-notification-link]");
    const badge = document.querySelector("[data-notification-badge]");
    if (!link || !badge) {
        return;
    }

    const countUrl = link.dataset.notificationCountUrl || "/api/v1/notifications/me";

    const setBadge = (count) => {
        const normalizedCount = Number(count || 0);
        if (!Number.isFinite(normalizedCount) || normalizedCount <= 0) {
            badge.hidden = true;
            badge.textContent = "0";
            return;
        }
        badge.hidden = false;
        badge.textContent = normalizedCount > 99 ? "99+" : String(normalizedCount);
    };

    const refreshUnreadCount = async () => {
        try {
            const params = new URLSearchParams({
                unreadOnly: "true",
                page: "1",
                size: "1"
            });
            const response = await fetch(`${countUrl}?${params.toString()}`, {
                credentials: "same-origin",
                headers: { Accept: "application/json" }
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || !payload || payload.success !== true) {
                setBadge(0);
                return;
            }
            setBadge(payload.data?.totalCount || 0);
        } catch (error) {
            setBadge(0);
        }
    };

    window.SanebNotifications = {
        ...(window.SanebNotifications || {}),
        refreshUnreadCount
    };

    refreshUnreadCount();
})();
