-- Company logos aren't used anywhere in the product — no admin UI ever renders one, and the
-- dedicated upload endpoint had no caller. Dropping the column and the whole upload feature outright.

ALTER TABLE company_profiles DROP COLUMN logo_url;
