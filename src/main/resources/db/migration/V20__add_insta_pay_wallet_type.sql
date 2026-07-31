ALTER TABLE customer_profiles DROP CONSTRAINT chk_customer_profiles_wallet_type;
ALTER TABLE customer_profiles ADD CONSTRAINT chk_customer_profiles_wallet_type
    CHECK (wallet_type IS NULL OR wallet_type IN ('VODAFONE_CASH', 'ETISALAT_CASH', 'INSTA_PAY'));

ALTER TABLE cashback_transactions DROP CONSTRAINT chk_cashback_wallet_type;
ALTER TABLE cashback_transactions ADD CONSTRAINT chk_cashback_wallet_type
    CHECK (wallet_type IN ('VODAFONE_CASH', 'ETISALAT_CASH', 'INSTA_PAY'));
