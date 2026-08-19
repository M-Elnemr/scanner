package com.umrah.scanner.hotel.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.hotel.application.CreateHotelUseCase;
import com.umrah.scanner.hotel.application.DeleteHotelUseCase;
import com.umrah.scanner.hotel.application.HotelCommand;
import com.umrah.scanner.hotel.application.HotelQueryService;
import com.umrah.scanner.hotel.application.UpdateHotelCommand;
import com.umrah.scanner.hotel.application.UpdateHotelUseCase;
import com.umrah.scanner.hotel.application.UploadHotelPhotoUseCase;
import com.umrah.scanner.hotel.domain.HotelCity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Hotels", description = "The Makkah/Madinah hotel catalogue a trip picks from.")
@RestController
public class HotelController {

    private final HotelQueryService hotelQueryService;
    private final CreateHotelUseCase createHotelUseCase;
    private final UpdateHotelUseCase updateHotelUseCase;
    private final DeleteHotelUseCase deleteHotelUseCase;
    private final UploadHotelPhotoUseCase uploadHotelPhotoUseCase;

    public HotelController(
            HotelQueryService hotelQueryService,
            CreateHotelUseCase createHotelUseCase,
            UpdateHotelUseCase updateHotelUseCase,
            DeleteHotelUseCase deleteHotelUseCase,
            UploadHotelPhotoUseCase uploadHotelPhotoUseCase) {
        this.hotelQueryService = hotelQueryService;
        this.createHotelUseCase = createHotelUseCase;
        this.updateHotelUseCase = updateHotelUseCase;
        this.deleteHotelUseCase = deleteHotelUseCase;
        this.uploadHotelPhotoUseCase = uploadHotelPhotoUseCase;
    }

    @Operation(summary = "List hotels", description = "Public. Active hotels only — the trip form's picker. "
            + "Pass city to scope it to Makkah or Madinah.")
    @GetMapping("/api/v1/hotels")
    public ApiResponse<List<HotelResponse>> list(@RequestParam(required = false) HotelCity city) {
        return ApiResponse.of(hotelQueryService.listActive(city).stream().map(HotelResponse::from).toList());
    }

    @Operation(summary = "List every hotel, including retired ones", description = "Admin-only.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/hotels")
    public ApiResponse<List<HotelResponse>> listAll() {
        return ApiResponse.of(hotelQueryService.listAll().stream().map(HotelResponse::from).toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/admin/hotels")
    public ApiResponse<HotelResponse> create(
            @AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody CreateHotelRequest request) {
        var command = new HotelCommand(
                request.city(), request.name(), request.nameAr(), request.stars(), request.distanceToHaramM(),
                request.canWalk(), request.locationUrl(), request.latitude(), request.longitude(), request.active());
        return ApiResponse.of(HotelResponse.from(createHotelUseCase.execute(admin.userId(), command)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/hotels/{id}")
    public ApiResponse<HotelResponse> update(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody UpdateHotelRequest request) {
        var command = new UpdateHotelCommand(
                request.name(), request.nameAr(), request.stars(), request.distanceToHaramM(),
                request.canWalk(), request.locationUrl(), request.latitude(), request.longitude(), request.active());
        return ApiResponse.of(HotelResponse.from(updateHotelUseCase.execute(admin.userId(), id, command)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/v1/admin/hotels/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HotelResponse> uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.of(HotelResponse.from(uploadHotelPhotoUseCase.execute(id, file)));
    }

    @Operation(summary = "Delete a hotel", description = "Refuses with 409 while any trip still uses it — "
            + "mark it inactive (PUT with active=false) instead.")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/admin/hotels/{id}")
    public void delete(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        deleteHotelUseCase.execute(admin.userId(), id);
    }
}
