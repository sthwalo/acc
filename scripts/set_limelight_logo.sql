-- Set logo for Limelight Academy Institutions
UPDATE companies
SET logo_path = 'input/logo.png'
WHERE name ILIKE 'Limelight Academy%';
