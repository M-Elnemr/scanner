-- One photo and precise coordinates per hotel, so the client can show real photos instead of the
-- tier-gradient placeholder, and link out to a map with the Kaaba/Prophet's Mosque as a reference
-- point instead of the free-text location_url a company used to paste in.

ALTER TABLE hotels
    ADD COLUMN photo_url VARCHAR(500),
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

COMMENT ON COLUMN hotels.photo_url IS
    'Uploaded by an admin — see UploadHotelPhotoUseCase. Stored the same way as a company logo:
     directly on disk under the uploads volume, not an external object store.';
COMMENT ON COLUMN hotels.latitude IS
    'Decimal degrees. Nullable — older/unedited hotels only have the free-text location_url until an
     admin sets this. Drives the "view on map" link, which also plots the Kaaba (Makkah) or the
     Prophet''s Mosque (Madinah) as a fixed reference point.';
COMMENT ON COLUMN hotels.longitude IS 'Decimal degrees — see latitude.';
