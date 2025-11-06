-- Migration: Create asset_disposals table for section 11(o) disposal tracking
-- Date: 6 November 2025
-- Description: Adds table to track asset disposals with loss/gain calculations per SARS guidelines

CREATE TABLE IF NOT EXISTS asset_disposals (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    disposal_date DATE NOT NULL,
    disposal_type VARCHAR(50) NOT NULL CHECK (disposal_type IN ('SALE', 'SCRAP', 'THEFT', 'DONATION', 'DESTRUCTION')),
    proceeds_received DECIMAL(15,2) DEFAULT 0.00,
    tax_value DECIMAL(15,2) NOT NULL, -- Cost - Accumulated Depreciation
    loss_on_disposal DECIMAL(15,2) DEFAULT 0.00, -- Section 11(o) allowance
    gain_on_disposal DECIMAL(15,2) DEFAULT 0.00, -- Recoupment under section 8(4)(a)
    reference VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    journal_entry_id BIGINT REFERENCES journal_entries(id),
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT positive_proceeds CHECK (proceeds_received >= 0),
    CONSTRAINT positive_tax_value CHECK (tax_value >= 0),
    CONSTRAINT non_negative_loss CHECK (loss_on_disposal >= 0),
    CONSTRAINT non_negative_gain CHECK (gain_on_disposal >= 0),
    CONSTRAINT loss_or_gain_only CHECK (
        (loss_on_disposal > 0 AND gain_on_disposal = 0) OR
        (gain_on_disposal > 0 AND loss_on_disposal = 0) OR
        (loss_on_disposal = 0 AND gain_on_disposal = 0)
    )
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_asset_disposals_asset_id ON asset_disposals(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_disposals_company_id ON asset_disposals(company_id);
CREATE INDEX IF NOT EXISTS idx_asset_disposals_disposal_date ON asset_disposals(disposal_date);
CREATE INDEX IF NOT EXISTS idx_asset_disposals_reference ON asset_disposals(reference);

-- Comments
COMMENT ON TABLE asset_disposals IS 'Tracks asset disposals with section 11(o) loss/gain calculations per SARS guidelines';
COMMENT ON COLUMN asset_disposals.tax_value IS 'Asset cost minus accumulated depreciation (tax value for disposal)';
COMMENT ON COLUMN asset_disposals.loss_on_disposal IS 'Section 11(o) allowance when tax value > proceeds';
COMMENT ON COLUMN asset_disposals.gain_on_disposal IS 'Recoupment under section 8(4)(a) when proceeds > tax value';

-- Insert sample data for testing (optional - remove in production)
-- This would be populated by the application when assets are disposed