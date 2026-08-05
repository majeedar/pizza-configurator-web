-- Backs real staff/admin login (replacing the gateway's hardcoded demo credentials).
-- Accounts are admin-created only — there is no public registration for this table,
-- unlike customers. Seeds exactly one default admin so a fresh deployment isn't
-- locked out (nobody could otherwise log in to create the first admin account);
-- must_change_password forces it to be rotated on first login. The password hash
-- below is bcrypt("ChangeMe123!") — document this default clearly, it is not a secret.
CREATE TABLE staff_accounts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    role                  VARCHAR(16)  NOT NULL,
    must_change_password  BOOLEAN      NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO staff_accounts (email, password_hash, full_name, role, must_change_password) VALUES
    ('admin@pizzaconfig.local', '$2b$10$8tWCGRooHxI5jx6sEmDwFeeBfxz6kDCp0XBMnmpdFaldkac1Sl7yW', 'Default Admin', 'ADMIN', true);
