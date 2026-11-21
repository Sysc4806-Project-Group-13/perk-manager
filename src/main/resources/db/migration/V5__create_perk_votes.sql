-- Stores votes submitted by users for individual perks
CREATE TABLE perk_votes (
                            user_id BIGINT NOT NULL,
                            perk_id BIGINT NOT NULL,

    -- 1 = upvote, -1 = downvote
                            vote_value SMALLINT NOT NULL,

    -- A user can only vote once per perk
                            CONSTRAINT pk_perk_votes
                                PRIMARY KEY (user_id, perk_id),

    -- Only allow upvotes and downvotes
                            CONSTRAINT chk_perk_votes_value
                                CHECK (vote_value IN (-1, 1)),

    -- Associate the vote with a user
                            CONSTRAINT fk_perk_votes_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

    -- Associate the vote with a perk
                            CONSTRAINT fk_perk_votes_perk
                                FOREIGN KEY (perk_id)
                                    REFERENCES perks(id)
                                    ON DELETE CASCADE
);

-- Allows efficient retrieval of votes for a perk
CREATE INDEX idx_perk_votes_perk_id
    ON perk_votes(perk_id);
