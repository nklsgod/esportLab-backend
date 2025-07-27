-- Safe team management migration that handles existing schemas

-- Make team_id nullable in member table if it isn't already
DO $$ 
BEGIN 
    -- Check if the column is already nullable
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'member' 
        AND column_name = 'team_id' 
        AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE member ALTER COLUMN team_id DROP NOT NULL;
    END IF;
END $$;

-- Create team_invite table only if it doesn't exist
CREATE TABLE IF NOT EXISTS team_invite (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    invite_code VARCHAR(16) UNIQUE NOT NULL,
    created_by_member_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    max_uses INTEGER NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add foreign key constraints only if they don't exist
DO $$
BEGIN
    -- Add team foreign key constraint
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_team_invite_team'
        AND table_name = 'team_invite'
    ) THEN
        ALTER TABLE team_invite 
        ADD CONSTRAINT fk_team_invite_team 
        FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE;
    END IF;
    
    -- Add member foreign key constraint  
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_team_invite_creator'
        AND table_name = 'team_invite'
    ) THEN
        ALTER TABLE team_invite 
        ADD CONSTRAINT fk_team_invite_creator 
        FOREIGN KEY (created_by_member_id) REFERENCES member(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes only if they don't exist
CREATE INDEX IF NOT EXISTS idx_team_invite_code ON team_invite(invite_code);
CREATE INDEX IF NOT EXISTS idx_team_invite_team_id ON team_invite(team_id);
CREATE INDEX IF NOT EXISTS idx_team_invite_expires_at ON team_invite(expires_at);

-- Create partial index for active invites
DROP INDEX IF EXISTS idx_team_invite_active;
CREATE INDEX idx_team_invite_active ON team_invite(is_active) WHERE is_active = true;

-- Add check constraints only if they don't exist
DO $$
BEGIN
    -- Check constraint for used_count
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_team_invite_used_count_positive'
        AND table_name = 'team_invite'
    ) THEN
        ALTER TABLE team_invite 
        ADD CONSTRAINT chk_team_invite_used_count_positive 
        CHECK (used_count >= 0);
    END IF;
    
    -- Check constraint for max_uses
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_team_invite_max_uses_positive'
        AND table_name = 'team_invite'
    ) THEN
        ALTER TABLE team_invite 
        ADD CONSTRAINT chk_team_invite_max_uses_positive 
        CHECK (max_uses IS NULL OR max_uses > 0);
    END IF;
END $$;