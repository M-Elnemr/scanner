package com.umrah.scanner.hotel.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Stores the uploaded bytes directly on disk (under a Docker volume), same as a company logo. */
@Service
public class UploadHotelPhotoUseCase {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES =
            Map.of("image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final HotelRepository hotelRepository;
    private final Path uploadsRoot;

    public UploadHotelPhotoUseCase(HotelRepository hotelRepository, @Value("${app.uploads.dir}") String uploadsDir) {
        this.hotelRepository = hotelRepository;
        this.uploadsRoot = Path.of(uploadsDir, "hotel-photos");
    }

    @Transactional
    public Hotel execute(UUID hotelId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("A photo file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("Photo must be 5MB or smaller");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ValidationException("Photo must be a PNG, JPEG, or WebP image");
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> NotFoundException.of("Hotel", hotelId));

        String filename = hotel.getId() + "." + extension;
        try {
            Files.createDirectories(uploadsRoot);
            try (var existing = Files.list(uploadsRoot)) {
                existing.filter(p -> p.getFileName().toString().startsWith(hotel.getId() + "."))
                        .forEach(UploadHotelPhotoUseCase::deleteQuietly);
            }
            file.transferTo(uploadsRoot.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store hotel photo", e);
        }

        hotel.setPhotoUrl("/uploads/hotel-photos/" + filename);
        return hotel;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup of a stale photo file under the old extension.
        }
    }
}
