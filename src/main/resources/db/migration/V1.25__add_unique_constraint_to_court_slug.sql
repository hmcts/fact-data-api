ALTER TABLE court
  ADD CONSTRAINT court_slug_unique UNIQUE (slug);
