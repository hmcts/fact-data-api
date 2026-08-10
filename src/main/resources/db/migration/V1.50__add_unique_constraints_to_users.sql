ALTER TABLE users
  ADD CONSTRAINT users_email_unique UNIQUE (email),
  ADD CONSTRAINT users_sso_id_unique UNIQUE (sso_id);
