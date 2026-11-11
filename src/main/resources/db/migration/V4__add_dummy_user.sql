-- add a dummy account to demonstrate use case
-- Username: username
-- Password: password (SHA-256)

-- add username and hashed password
INSERT INTO users (username, password_hash)
VALUES (
           'username',
           '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8'
       );

-- give the user 2 memberships
INSERT INTO user_memberships (user_id, membership_id)
SELECT u.id, m.id
FROM users u
         CROSS JOIN memberships m
WHERE u.username = 'username'
  AND m.name IN ('Air Miles', 'Visa');