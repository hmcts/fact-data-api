-- Update court table to rename temporary_urgent_notice to warning_notice
ALTER TABLE court
  RENAME COLUMN temporary_urgent_notice TO warning_notice;
