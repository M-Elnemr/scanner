package com.umrah.scanner.notification.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.notification.application.NotificationQueryService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<NotificationResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(
                notificationQueryService.listForUser(currentUser.userId(), pageable), NotificationResponse::from));
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.of(notificationQueryService.countUnread(currentUser.userId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        return ApiResponse.of(NotificationResponse.from(notificationQueryService.markRead(id, currentUser.userId())));
    }
}
