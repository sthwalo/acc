-- V4__Add_user_companies_table.sql
-- Add user_companies table for Spring Boot user-company relationships

CREATE TABLE IF NOT EXISTS public.user_companies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_user_companies_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_companies_company_id FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT uk_user_companies_user_company UNIQUE (user_id, company_id)
);

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_user_companies_user_id ON public.user_companies(user_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_company_id ON public.user_companies(company_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_active ON public.user_companies(is_active);

-- Add trigger for updated_at
CREATE TRIGGER update_user_companies_updated_at
    BEFORE UPDATE ON public.user_companies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default user-company relationships for existing users and companies
-- Assuming user with ID 1 and companies exist
INSERT INTO public.user_companies (user_id, company_id, role, created_by, updated_by)
SELECT u.id, c.id, 'ADMIN', 'FIN', 'FIN'
FROM users u
CROSS JOIN companies c
WHERE u.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  )
ON CONFLICT (user_id, company_id) DO NOTHING;