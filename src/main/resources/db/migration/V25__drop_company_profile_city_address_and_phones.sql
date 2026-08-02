-- city/address are replaced by company_addresses (a list, each with its own city/address/mobile number).
DROP INDEX idx_company_profiles_city;
ALTER TABLE company_profiles DROP COLUMN city;
ALTER TABLE company_profiles DROP COLUMN address;

-- phone numbers are replaced by the mobile_number carried on each company_addresses row.
DROP TABLE company_phones;
