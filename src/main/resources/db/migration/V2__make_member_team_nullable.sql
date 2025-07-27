-- Make team_id nullable in member table to allow users to register without a team
ALTER TABLE member ALTER COLUMN team_id DROP NOT NULL;