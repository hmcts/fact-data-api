INSERT INTO contact_description_types (id, name, name_cy)
SELECT
  gen_random_uuid(),
  source.name,
  'CHANGE ME / NEED WELSH'
FROM (
       VALUES
         ('Acknowledgement of service'),
         ('Application enquiries'),
         ('Attachment of earnings applications'),
         ('Charging orders'),
         ('Claim responses'),
         ('Directions questionnaires'),
         ('Help with Fees'),
         ('Ongoing attachment of earnings order issues'),
         ('Request a certificate of satisfaction'),
         ('Request for judgment'),
         ('Scottish enquiries'),
         ('Traffic enforcement centre enquiries'),
         ('Warrants and writs of control')
     ) AS source(name)
WHERE NOT EXISTS (
  SELECT 1
  FROM contact_description_types cdt
  WHERE cdt.name = source.name
);

