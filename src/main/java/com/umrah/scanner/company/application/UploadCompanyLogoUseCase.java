package com.umrah.scanner.company.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Stores the uploaded bytes directly on disk (under a Docker volume) — no external object storage configured. */
@Service
public class UploadCompanyLogoUseCase {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES =
            Map.of("image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final CompanyProfileRepository companyProfileRepository;
    private final Path uploadsRoot;

    public UploadCompanyLogoUseCase(
            CompanyProfileRepository companyProfileRepository, @Value("${app.uploads.dir}") String uploadsDir) {
        this.companyProfileRepository = companyProfileRepository;
        this.uploadsRoot = Path.of(uploadsDir, "company-logos");
    }

    @Transactional
    public CompanyProfile execute(UUID companyUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("A logo file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("Logo must be 5MB or smaller");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ValidationException("Logo must be a PNG, JPEG, or WebP image");
        }

        CompanyProfile company = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId));

        String filename = company.getId() + "." + extension;
        try {
            Files.createDirectories(uploadsRoot);
            // Drop any previous logo saved under a different extension before writing the new one.
            try (var existing = Files.list(uploadsRoot)) {
                existing.filter(p -> p.getFileName().toString().startsWith(company.getId() + "."))
                        .forEach(UploadCompanyLogoUseCase::deleteQuietly);
            }
            file.transferTo(uploadsRoot.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store company logo", e);
        }

        company.setLogoUrl("/uploads/company-logos/" + filename);
        return company;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup of a stale logo file under the old extension — not worth failing the upload over.
        }
    }
}
