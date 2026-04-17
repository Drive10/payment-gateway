-- Normalize legacy payment-service schema to current UUID-based model.
-- This migration is defensive so local/hybrid environments can boot cleanly even with older schemas.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'disputes'
          AND column_name = 'payment_id'
          AND udt_name <> 'uuid'
    ) THEN
        EXECUTE $sql$
            ALTER TABLE disputes
            ALTER COLUMN payment_id TYPE UUID
            USING (
                CASE
                    WHEN payment_id IS NULL THEN NULL
                    WHEN payment_id::text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                        THEN payment_id::text::uuid
                    ELSE (
                        substr(md5(payment_id::text), 1, 8) || '-' ||
                        substr(md5(payment_id::text), 9, 4) || '-' ||
                        substr(md5(payment_id::text), 13, 4) || '-' ||
                        substr(md5(payment_id::text), 17, 4) || '-' ||
                        substr(md5(payment_id::text), 21, 12)
                    )::uuid
                END
            )
        $sql$;
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'disputes'
          AND column_name = 'merchant_id'
          AND udt_name <> 'uuid'
    ) THEN
        EXECUTE $sql$
            ALTER TABLE disputes
            ALTER COLUMN merchant_id TYPE UUID
            USING (
                CASE
                    WHEN merchant_id IS NULL THEN NULL
                    WHEN merchant_id::text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                        THEN merchant_id::text::uuid
                    ELSE (
                        substr(md5(merchant_id::text), 1, 8) || '-' ||
                        substr(md5(merchant_id::text), 9, 4) || '-' ||
                        substr(md5(merchant_id::text), 13, 4) || '-' ||
                        substr(md5(merchant_id::text), 17, 4) || '-' ||
                        substr(md5(merchant_id::text), 21, 12)
                    )::uuid
                END
            )
        $sql$;
    END IF;
END
$$;

DO $$
DECLARE
    fk_record RECORD;
BEGIN
    FOR fk_record IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
           AND tc.table_schema = kcu.table_schema
        JOIN information_schema.constraint_column_usage ccu
            ON tc.constraint_name = ccu.constraint_name
           AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'orders'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'user_id'
          AND ccu.table_name = 'users'
    LOOP
        EXECUTE format('ALTER TABLE orders DROP CONSTRAINT IF EXISTS %I', fk_record.constraint_name);
    END LOOP;
END
$$;
