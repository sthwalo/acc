-- Migration: Grant existing user ADMIN access to all existing companies
-- This ensures the current user (sthwaloe@gmail.com) retains access to all companies
-- Run this once after implementing user-company isolation

-- Get the user ID for the existing user
DO $$
DECLARE
    existing_user_id BIGINT;
    company_record RECORD;
BEGIN
    -- Find the existing user (assuming email is unique)
    SELECT id INTO existing_user_id 
    FROM users 
    WHERE email = 'sthwaloe@gmail.com' 
    LIMIT 1;
    
    -- Check if user exists
    IF existing_user_id IS NULL THEN
        RAISE NOTICE 'User sthwaloe@gmail.com not found. Skipping migration.';
        RETURN;
    END IF;
    
    RAISE NOTICE 'Found user ID: % for email: sthwaloe@gmail.com', existing_user_id;
    
    -- Grant ADMIN access to all existing companies for this user
    FOR company_record IN SELECT id, name FROM companies LOOP
        -- Insert or update user-company relationship
        INSERT INTO user_companies (user_id, company_id, role, is_active, created_at, updated_at, created_by, updated_by)
        VALUES (existing_user_id, company_record.id, 'ADMIN', true, NOW(), NOW(), 'SYSTEM_MIGRATION', 'SYSTEM_MIGRATION')
        ON CONFLICT (user_id, company_id) 
        DO UPDATE SET 
            role = 'ADMIN',
            is_active = true,
            updated_at = NOW(),
            updated_by = 'SYSTEM_MIGRATION';
        
        RAISE NOTICE 'Granted ADMIN access to company: % (ID: %) for user: sthwaloe@gmail.com', 
                    company_record.name, company_record.id;
    END LOOP;
    
    RAISE NOTICE 'Migration completed successfully. User now has ADMIN access to all companies.';
END $$;
