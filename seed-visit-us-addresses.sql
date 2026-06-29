-- Seed accurate location data for searches around PL12 4ER.
--
-- Court coordinates come from the final legacy fact-api search_court data
-- after V211__update_photo_lat_long_as_prod.sql. Postcodes come from the
-- preferred legacy "Visit us" or "Visit or contact us" address.
--
-- The court list contains the nearest open legacy courts needed to reproduce
-- the first 10 results for the seeded postcode searches around PL12 4ER.
--
-- Service-centre postcode searches only include LOCAL and REGIONAL
-- catchments. Bury St Edmunds Regional Divorce Centre uses its legacy court
-- coordinates. Online Civil Money Claims uses the legacy Harlow postcode and
-- the Harlow coordinates recorded for the Probate Service Centre because its
-- own legacy court coordinates are null.
--
-- The legacy model has 24 unique service-centre locations when combining:
--   * explicit search_servicecentre rows; and
--   * courts with LOCAL, REGIONAL, or NATIONAL service-area catchments.
--
-- Immigration and Asylum Appeals Service Centre and Single Justice Procedures
-- Service Centre are national, non-in-person services with no source address
-- or coordinates. They are created without fabricated WRITE_TO_US addresses.
--
-- Bury St Edmunds is the current all-England-and-Wales civil-partnership paper
-- service. The other retained regional divorce-centre records are therefore
-- not linked to the Civil partnership service area.
--
-- The script starts by deleting every persistent row it owns, so it can be
-- rerun after partial, successful, or manually modified previous executions.

BEGIN;

CREATE TEMP TABLE seed_owned_court (
  slug VARCHAR PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO seed_owned_court (slug)
VALUES
  ('plymouth-combined-court'),
  ('plymouth-magistrates-court'),
  ('plymouth-social-security-and-child-support-tribunal'),
  ('bodmin-law-courts'),
  ('newton-abbot-magistrates-court'),
  ('torquay-and-newton-abbot-county-and-family-court'),
  ('truro-magistrates-court'),
  ('exeter-law-courts'),
  ('exeter-social-security-and-child-support-tribunal'),
  ('truro-combined-court'),
  ('barnstaple-magistrates-county-and-family-court'),
  ('taunton-crown-county-and-family-court'),
  ('taunton-magistrates-court-tribunals-and-family-hearing-centre'),
  ('yeovil-county-family-and-magistrates-court'),
  ('weymouth-combined-court'),
  ('port-talbot-justice-centre'),
  ('swansea-crown-court'),
  ('swansea-civil-justice-centre'),
  ('swansea-magistrates-court'),
  ('cardiff-magistrates-court'),
  ('wales-employment-tribunal'),
  ('llanelli-law-courts'),
  ('carmarthen-county-court-and-family-court'),
  ('cardiff-civil-and-family-justice-centre'),
  ('midlands-west-employment-tribunal'),
  ('taylor-house-tribunal-hearing-centre'),
  ('manchester-employment-tribunal'),
  ('edinburgh-upper-tribunal-administrative-appeals-chamber'),
  ('newport-south-wales-county-court-and-family-court'),
  ('bristol-civil-and-family-justice-centre'),
  ('bournemouth-combined-court'),
  ('southampton-combined-court-centre'),
  ('oxford-combined-court-centre'),
  ('birmingham-civil-and-family-justice-centre'),
  ('wrexham-county-and-family-court'),
  ('central-family-court'),
  ('portsmouth-combined-court-centre'),
  ('reading-county-court-and-family-court'),
  ('west-london-family-court'),
  ('brighton-hearing-centre'),
  ('luton-justice-centre'),
  ('pontypridd-county-court-and-family-court'),
  ('newport-south-wales-immigration-and-asylum-tribunal'),
  ('birmingham-immigration-and-asylum-chamber-first-tier-tribunal'),
  ('harmondsworth-tribunal-hearing-centre'),
  ('hatton-cross-tribunal-hearing-centre'),
  ('coventry-magistrates-court'),
  ('field-house-tribunal-hearing-centre'),
  ('yarls-wood-immigration-and-asylum-hearing-centre');

CREATE TEMP TABLE seed_owned_court_authority (
  slug VARCHAR PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO seed_owned_court_authority (slug)
VALUES
  ('barnstaple-magistrates-county-and-family-court'),
  ('exeter-law-courts'),
  ('plymouth-combined-court'),
  ('taunton-crown-county-and-family-court'),
  ('torquay-and-newton-abbot-county-and-family-court'),
  ('truro-combined-court');

-- Clear every persistent row owned by this script before rebuilding it.
DELETE FROM court_address ca
USING court c, seed_owned_court seed
WHERE ca.court_id = c.id
  AND c.slug = seed.slug
  AND ca.address_type = 'VISIT_US';

DELETE FROM court_single_points_of_entry spoe
USING court c, seed_owned_court_authority seed
WHERE spoe.court_id = c.id
  AND c.slug = seed.slug;

DELETE FROM court_local_authorities cla
USING court c, seed_owned_court_authority seed
WHERE cla.court_id = c.id
  AND c.slug = seed.slug;

DELETE FROM court_areas_of_law coa
USING court c, seed_owned_court seed
WHERE coa.court_id = c.id
  AND c.slug = seed.slug;

-- The service-centre aggregate is new and entirely rebuilt by this seed.
-- Child address/contact/area-of-law rows are removed by ON DELETE CASCADE.
DELETE FROM service_centre;

CREATE TEMP TABLE seed_court_location (
  slug VARCHAR PRIMARY KEY,
  town_city VARCHAR NOT NULL,
  county VARCHAR NOT NULL,
  postcode VARCHAR NOT NULL,
  lat DECIMAL(9,6) NOT NULL,
  lon DECIMAL(9,6) NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_court_location (slug, town_city, county, postcode, lat, lon)
VALUES
  ('plymouth-combined-court', 'Plymouth', 'Devon', 'PL1 2ER', 50.369194, -4.141694),
  ('plymouth-magistrates-court', 'Plymouth', 'Devon', 'PL1 2DP', 50.369287, -4.139575),
  (
    'plymouth-social-security-and-child-support-tribunal',
    'Plymouth',
    'Devon',
    'PL1 2TS',
    50.368462,
    -4.144095
  ),
  ('bodmin-law-courts', 'Bodmin', 'Cornwall', 'PL31 2AL', 50.469527, -4.708865),
  ('newton-abbot-magistrates-court', 'Newton Abbot', 'Devon', 'TQ12 1LA', 50.527693, -3.613000),
  (
    'torquay-and-newton-abbot-county-and-family-court',
    'Torquay',
    'Devon',
    'TQ2 7AZ',
    50.486767,
    -3.551821
  ),
  ('truro-magistrates-court', 'Truro', 'Cornwall', 'TR1 1HZ', 50.268809, -5.043798),
  ('exeter-law-courts', 'Exeter', 'Devon', 'EX1 1UH', 50.721273, -3.525229),
  (
    'exeter-social-security-and-child-support-tribunal',
    'Exeter',
    'Devon',
    'EX1 1NT',
    50.721826,
    -3.525447
  ),
  ('truro-combined-court', 'Truro', 'Cornwall', 'TR1 2PB', 50.265626, -5.056408),
  (
    'barnstaple-magistrates-county-and-family-court',
    'Barnstaple',
    'Devon',
    'EX31 1DU',
    51.081265,
    -4.065455
  ),
  ('taunton-crown-county-and-family-court', 'Taunton', 'Somerset', 'TA1 4EU', 51.012768, -3.107709),
  (
    'taunton-magistrates-court-tribunals-and-family-hearing-centre',
    'Taunton',
    'Somerset',
    'TA1 4AX',
    51.014297,
    -3.109670
  ),
  (
    'yeovil-county-family-and-magistrates-court',
    'Yeovil',
    'Somerset',
    'BA20 1SW',
    50.939623,
    -2.633787
  ),
  ('weymouth-combined-court', 'Weymouth', 'Dorset', 'DT4 8BS', 50.608402, -2.460106),
  (
    'port-talbot-justice-centre',
    'Port Talbot',
    'Neath Port Talbot',
    'SA13 1SB',
    51.590648,
    -3.785950
  ),
  ('swansea-crown-court', 'Swansea', 'Swansea', 'SA1 4PF', 51.614438, -3.957541),
  (
    'swansea-civil-justice-centre',
    'Swansea',
    'Swansea',
    'SA1 1SP',
    51.620176,
    -3.938026
  ),
  (
    'swansea-magistrates-court',
    'Swansea',
    'Swansea',
    'SA1 5DB',
    51.621879,
    -3.945379
  ),
  (
    'cardiff-magistrates-court',
    'Cardiff',
    'Cardiff',
    'CF24 0RZ',
    51.482105,
    -3.166483
  ),
  (
    'wales-employment-tribunal',
    'Cardiff',
    'Cardiff',
    'CF24 0RZ',
    51.482105,
    -3.166483
  ),
  (
    'llanelli-law-courts',
    'Llanelli',
    'Carmarthenshire',
    'SA15 3AW',
    51.682104,
    -4.163143
  ),
  (
    'carmarthen-county-court-and-family-court',
    'Carmarthen',
    'Carmarthenshire',
    'SA31 3BT',
    51.854977,
    -4.317909
  ),
  (
    'cardiff-civil-and-family-justice-centre',
    'Cardiff',
    'Cardiff',
    'CF10 1ET',
    51.478158,
    -3.179875
  ),
  (
    'midlands-west-employment-tribunal',
    'Birmingham',
    'West Midlands',
    'B5 4UU',
    52.476224,
    -1.898516
  ),
  (
    'taylor-house-tribunal-hearing-centre',
    'London',
    'Greater London',
    'EC1R 4QU',
    51.527396,
    -0.107442
  ),
  (
    'manchester-employment-tribunal',
    'Manchester',
    'Greater Manchester',
    'M3 2JA',
    53.482953,
    -2.248295
  ),
  (
    'edinburgh-upper-tribunal-administrative-appeals-chamber',
    'Edinburgh',
    'City of Edinburgh',
    'EH3 7HF',
    55.951707,
    -3.205070
  ),
  (
    'newport-south-wales-county-court-and-family-court',
    'Newport',
    'Monmouthshire',
    'NP19 7AA',
    51.590942,
    -2.992493
  ),
  (
    'bristol-civil-and-family-justice-centre',
    'Bristol',
    'Somerset',
    'BS1 6GR',
    51.452423,
    -2.590510
  ),
  (
    'bournemouth-combined-court',
    'Bournemouth',
    'Dorset',
    'BH7 7DS',
    50.747943,
    -1.816266
  ),
  (
    'southampton-combined-court-centre',
    'Southampton',
    'Hampshire',
    'SO15 2XQ',
    50.913706,
    -1.402977
  ),
  (
    'oxford-combined-court-centre',
    'Oxford',
    'Oxfordshire',
    'OX1 1TL',
    51.748170,
    -1.257072
  ),
  (
    'birmingham-civil-and-family-justice-centre',
    'Birmingham',
    'West Midlands',
    'B4 6DS',
    52.481652,
    -1.895662
  ),
  (
    'wrexham-county-and-family-court',
    'Wrexham',
    'Denbighshire',
    'LL12 7BP',
    53.048460,
    -2.989528
  ),
  (
    'central-family-court',
    'London',
    'Greater London',
    'WC1V 6NP',
    51.518436,
    -0.114516
  ),
  (
    'portsmouth-combined-court-centre',
    'Portsmouth',
    'Hampshire',
    'PO1 2EB',
    50.795753,
    -1.091163
  ),
  (
    'reading-county-court-and-family-court',
    'Reading',
    'Berkshire',
    'RG1 1HE',
    51.456575,
    -0.971179
  ),
  (
    'west-london-family-court',
    'Feltham',
    'Greater London',
    'TW14 0LR',
    51.458536,
    -0.412621
  ),
  (
    'brighton-hearing-centre',
    'Brighton',
    'East Sussex',
    'BN2 0JD',
    50.822983,
    -0.135443
  ),
  (
    'luton-justice-centre',
    'Luton',
    'Bedfordshire',
    'LU1 2LJ',
    51.879593,
    -0.414441
  ),
  (
    'pontypridd-county-court-and-family-court',
    'Pontypridd',
    'Rhondda Cynon Taff',
    'CF37 1JR',
    51.599776,
    -3.344303
  ),
  (
    'newport-south-wales-immigration-and-asylum-tribunal',
    'Newport',
    'Monmouthshire',
    'NP18 2LX',
    51.603080,
    -2.919666
  ),
  (
    'birmingham-immigration-and-asylum-chamber-first-tier-tribunal',
    'Birmingham',
    'West Midlands',
    'B4 6DS',
    52.481652,
    -1.895662
  ),
  (
    'harmondsworth-tribunal-hearing-centre',
    'Harmondsworth',
    'Greater London',
    'UB7 0HD',
    51.484605,
    -0.483988
  ),
  (
    'hatton-cross-tribunal-hearing-centre',
    'Feltham',
    'Greater London',
    'TW14 0LS',
    51.458160,
    -0.414073
  ),
  (
    'coventry-magistrates-court',
    'Coventry',
    'Warwickshire',
    'CV1 2SQ',
    52.405831,
    -1.507628
  ),
  (
    'field-house-tribunal-hearing-centre',
    'London',
    'Greater London',
    'EC4A 1DZ',
    51.516287,
    -0.110035
  ),
  (
    'yarls-wood-immigration-and-asylum-hearing-centre',
    'Bedford',
    'Bedfordshire',
    'MK44 1FD',
    52.196587,
    -0.489819
  );

UPDATE court c
SET open = TRUE
FROM seed_court_location source
WHERE c.slug = source.slug;

UPDATE court_address ca
SET address_line_1 = c.name,
    town_city = source.town_city,
    county = source.county,
    postcode = source.postcode,
    lat = source.lat,
    lon = source.lon
FROM court c
JOIN seed_court_location source ON source.slug = c.slug
WHERE ca.court_id = c.id
  AND ca.address_type = 'VISIT_US';

INSERT INTO court_address (
  id,
  court_id,
  address_line_1,
  town_city,
  county,
  postcode,
  lat,
  lon,
  address_type
)
SELECT
  gen_random_uuid(),
  c.id,
  c.name,
  source.town_city,
  source.county,
  source.postcode,
  source.lat,
  source.lon,
  'VISIT_US'
FROM court c
JOIN seed_court_location source ON source.slug = c.slug
WHERE NOT EXISTS (
  SELECT 1
  FROM court_address ca
  WHERE ca.court_id = c.id
    AND ca.address_type = 'VISIT_US'
);

CREATE TEMP TABLE seed_court_authority_mapping (
  slug VARCHAR NOT NULL,
  area_of_law_name VARCHAR NOT NULL,
  local_authority_names VARCHAR[] NOT NULL,
  is_spoe BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (slug, area_of_law_name)
) ON COMMIT DROP;

INSERT INTO seed_court_authority_mapping (
  slug,
  area_of_law_name,
  local_authority_names,
  is_spoe
)
VALUES
  (
    'barnstaple-magistrates-county-and-family-court',
    'Children',
    ARRAY['Devon County Council'],
    FALSE
  ),
  (
    'exeter-law-courts',
    'Adoption',
    ARRAY['Devon County Council', 'Torbay Council'],
    FALSE
  ),
  (
    'exeter-law-courts',
    'Children',
    ARRAY['Devon County Council'],
    TRUE
  ),
  (
    'plymouth-combined-court',
    'Adoption',
    ARRAY['Devon County Council', 'Plymouth City Council', 'Torbay Council'],
    FALSE
  ),
  (
    'plymouth-combined-court',
    'Children',
    ARRAY['Devon County Council', 'Plymouth City Council', 'Torbay Council'],
    TRUE
  ),
  (
    'taunton-crown-county-and-family-court',
    'Adoption',
    ARRAY['Somerset County Council'],
    FALSE
  ),
  (
    'taunton-crown-county-and-family-court',
    'Children',
    ARRAY['Somerset County Council'],
    TRUE
  ),
  (
    'torquay-and-newton-abbot-county-and-family-court',
    'Children',
    ARRAY['Devon County Council', 'Plymouth City Council', 'Torbay Council'],
    TRUE
  ),
  (
    'truro-combined-court',
    'Adoption',
    ARRAY['Cornwall Council', 'Isles of Scilly Council'],
    FALSE
  ),
  (
    'truro-combined-court',
    'Children',
    ARRAY['Cornwall Council', 'Isles of Scilly Council'],
    TRUE
  );

CREATE TEMP TABLE seed_court_area_of_law (
  slug VARCHAR PRIMARY KEY,
  area_of_law_names VARCHAR[] NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_court_area_of_law (slug, area_of_law_names)
VALUES
  (
    'barnstaple-magistrates-county-and-family-court',
    ARRAY[
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'bodmin-law-courts',
    ARRAY[
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  ),
  (
    'exeter-law-courts',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'newton-abbot-magistrates-court',
    ARRAY['Crime', 'Single justice procedure', 'Social security']
  ),
  (
    'plymouth-combined-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Financial Remedy',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  ),
  (
    'plymouth-magistrates-court',
    ARRAY[
      'Children',
      'Crime',
      'Domestic violence',
      'Employment',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'plymouth-social-security-and-child-support-tribunal',
    ARRAY['Social security']
  ),
  (
    'port-talbot-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Civil partnership',
      'Domestic violence',
      'Employment',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Social security'
    ]
  ),
  (
    'swansea-crown-court',
    ARRAY['Crime', 'Single justice procedure']
  ),
  (
    'swansea-civil-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Domestic violence',
      'Employment',
      'Financial Remedy',
      'Housing possession',
      'Money claims'
    ]
  ),
  (
    'swansea-magistrates-court',
    ARRAY['Crime', 'Employment', 'Single justice procedure']
  ),
  (
    'cardiff-magistrates-court',
    ARRAY['Crime', 'Employment', 'Immigration', 'Single justice procedure']
  ),
  (
    'wales-employment-tribunal',
    ARRAY['Employment']
  ),
  (
    'llanelli-law-courts',
    ARRAY[
      'Children',
      'Crime',
      'Domestic violence',
      'Employment',
      'Housing possession',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'carmarthen-county-court-and-family-court',
    ARRAY[
      'Bankruptcy',
      'Children',
      'Domestic violence',
      'Employment',
      'High Court District Registry',
      'Housing possession'
    ]
  ),
  (
    'cardiff-civil-and-family-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Domestic violence',
      'FGM',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Social security'
    ]
  ),
  (
    'midlands-west-employment-tribunal',
    ARRAY['Employment', 'Tax']
  ),
  (
    'taylor-house-tribunal-hearing-centre',
    ARRAY['Immigration', 'Tax']
  ),
  (
    'manchester-employment-tribunal',
    ARRAY['Employment', 'Tax']
  ),
  (
    'edinburgh-upper-tribunal-administrative-appeals-chamber',
    ARRAY['Social security', 'Tax']
  ),
  (
    'newport-south-wales-county-court-and-family-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Civil partnership',
      'Divorce',
      'Domestic violence',
      'Financial Remedy',
      'High Court District Registry',
      'Housing possession'
    ]
  ),
  (
    'bristol-civil-and-family-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Divorce',
      'Domestic violence',
      'Employment',
      'FGM',
      'Financial Remedy',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Social security'
    ]
  ),
  (
    'bournemouth-combined-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'Financial Remedy',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  ),
  (
    'southampton-combined-court-centre',
    ARRAY[
      'Bankruptcy',
      'Children',
      'Crime',
      'Domestic violence',
      'Employment',
      'Financial Remedy',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'oxford-combined-court-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Financial Remedy',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  ),
  (
    'birmingham-civil-and-family-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Business and Property',
      'Children',
      'Civil partnership',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Financial Remedy',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Immigration',
      'Money claims',
      'Social security'
    ]
  ),
  (
    'wrexham-county-and-family-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Civil partnership',
      'Divorce',
      'Domestic violence',
      'Financial Remedy',
      'Housing possession'
    ]
  ),
  (
    'central-family-court',
    ARRAY[
      'Adoption',
      'Children',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Financial Remedy',
      'Forced marriage'
    ]
  ),
  (
    'portsmouth-combined-court-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'reading-county-court-and-family-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims'
    ]
  ),
  (
    'west-london-family-court',
    ARRAY[
      'Adoption',
      'Children',
      'Domestic violence',
      'FGM',
      'Forced marriage'
    ]
  ),
  (
    'brighton-hearing-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Divorce',
      'Domestic violence',
      'FGM',
      'Forced marriage',
      'Housing possession',
      'Money claims'
    ]
  ),
  (
    'luton-justice-centre',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Domestic violence',
      'FGM',
      'Forced marriage',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Social security'
    ]
  ),
  (
    'pontypridd-county-court-and-family-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession'
    ]
  ),
  (
    'newport-south-wales-immigration-and-asylum-tribunal',
    ARRAY['Immigration']
  ),
  (
    'birmingham-immigration-and-asylum-chamber-first-tier-tribunal',
    ARRAY['Immigration']
  ),
  (
    'harmondsworth-tribunal-hearing-centre',
    ARRAY['Immigration']
  ),
  (
    'hatton-cross-tribunal-hearing-centre',
    ARRAY['Immigration', 'Social security']
  ),
  (
    'coventry-magistrates-court',
    ARRAY[
      'Children',
      'Crime',
      'Immigration',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'field-house-tribunal-hearing-centre',
    ARRAY['Immigration']
  ),
  (
    'yarls-wood-immigration-and-asylum-hearing-centre',
    ARRAY['Immigration']
  ),
  (
    'taunton-crown-county-and-family-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  ),
  (
    'taunton-magistrates-court-tribunals-and-family-hearing-centre',
    ARRAY['Crime', 'Employment', 'Single justice procedure', 'Social security']
  ),
  (
    'torquay-and-newton-abbot-county-and-family-court',
    ARRAY['Bankruptcy', 'Children', 'Divorce', 'Housing possession', 'Money claims']
  ),
  (
    'truro-combined-court',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'truro-magistrates-court',
    ARRAY['Crime', 'Employment', 'Single justice procedure', 'Social security']
  ),
  (
    'exeter-social-security-and-child-support-tribunal',
    ARRAY['Social security']
  ),
  (
    'weymouth-combined-court',
    ARRAY[
      'Children',
      'Crime',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure',
      'Social security'
    ]
  ),
  (
    'yeovil-county-family-and-magistrates-court',
    ARRAY[
      'Bankruptcy',
      'Children',
      'Crime',
      'Divorce',
      'Domestic violence',
      'High Court District Registry',
      'Housing possession',
      'Money claims',
      'Single justice procedure'
    ]
  );

INSERT INTO court_areas_of_law (
  id,
  court_id,
  areas_of_law
)
SELECT
  gen_random_uuid(),
  c.id,
  ARRAY_AGG(DISTINCT aol.id ORDER BY aol.id)
FROM court c
JOIN seed_court_area_of_law seed ON seed.slug = c.slug
JOIN area_of_law_types aol ON aol.name = ANY(seed.area_of_law_names)
GROUP BY c.id;

UPDATE court_single_points_of_entry spoe
SET areas_of_law = source.area_of_law_ids
FROM (
  SELECT
    c.id AS court_id,
    ARRAY(
      SELECT DISTINCT value
      FROM unnest(
        COALESCE(spoe_existing.areas_of_law, ARRAY[]::UUID[])
        || ARRAY_AGG(DISTINCT aol.id)
      ) value
      ORDER BY value
    ) AS area_of_law_ids
  FROM court c
  JOIN seed_court_authority_mapping seed ON seed.slug = c.slug AND seed.is_spoe
  JOIN area_of_law_types aol ON aol.name = seed.area_of_law_name
  LEFT JOIN court_single_points_of_entry spoe_existing ON spoe_existing.court_id = c.id
  GROUP BY c.id, spoe_existing.areas_of_law
) source
WHERE spoe.court_id = source.court_id;

INSERT INTO court_single_points_of_entry (
  id,
  court_id,
  areas_of_law
)
SELECT
  gen_random_uuid(),
  c.id,
  ARRAY_AGG(DISTINCT aol.id ORDER BY aol.id)
FROM court c
JOIN seed_court_authority_mapping seed ON seed.slug = c.slug AND seed.is_spoe
JOIN area_of_law_types aol ON aol.name = seed.area_of_law_name
WHERE NOT EXISTS (
  SELECT 1
  FROM court_single_points_of_entry spoe
  WHERE spoe.court_id = c.id
)
GROUP BY c.id;

UPDATE court_local_authorities cla
SET local_authority_ids = source.local_authority_ids
FROM (
  SELECT
    c.id AS court_id,
    aol.id AS area_of_law_id,
    ARRAY(
      SELECT DISTINCT value
      FROM unnest(
        COALESCE(cla_existing.local_authority_ids, ARRAY[]::UUID[])
        || ARRAY_AGG(DISTINCT lat.id)
      ) value
      ORDER BY value
    ) AS local_authority_ids
  FROM court c
  JOIN seed_court_authority_mapping seed ON seed.slug = c.slug
  JOIN area_of_law_types aol ON aol.name = seed.area_of_law_name
  JOIN local_authority_types lat ON lat.name = ANY(seed.local_authority_names)
  LEFT JOIN court_local_authorities cla_existing
    ON cla_existing.court_id = c.id
    AND cla_existing.area_of_law_id = aol.id
  GROUP BY c.id, aol.id, cla_existing.local_authority_ids
) source
WHERE cla.court_id = source.court_id
  AND cla.area_of_law_id = source.area_of_law_id;

INSERT INTO court_local_authorities (
  id,
  court_id,
  area_of_law_id,
  local_authority_ids
)
SELECT
  gen_random_uuid(),
  c.id,
  aol.id,
  ARRAY_AGG(DISTINCT lat.id ORDER BY lat.id)
FROM court c
JOIN seed_court_authority_mapping seed ON seed.slug = c.slug
JOIN area_of_law_types aol ON aol.name = seed.area_of_law_name
JOIN local_authority_types lat ON lat.name = ANY(seed.local_authority_names)
WHERE NOT EXISTS (
  SELECT 1
  FROM court_local_authorities cla
  WHERE cla.court_id = c.id
    AND cla.area_of_law_id = aol.id
)
GROUP BY c.id, aol.id;

CREATE TEMP TABLE seed_service_centre_location (
  slug VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL,
  catchment_type VARCHAR,
  service_area_names VARCHAR[] NOT NULL,
  address_line_1 VARCHAR,
  town_city VARCHAR,
  county VARCHAR,
  postcode VARCHAR,
  lat DECIMAL(9,6),
  lon DECIMAL(9,6)
) ON COMMIT DROP;

INSERT INTO seed_service_centre_location (
  slug,
  name,
  catchment_type,
  service_area_names,
  address_line_1,
  town_city,
  county,
  postcode,
  lat,
  lon
)
VALUES
  (
    'bury-st-edmunds-regional-divorce-centre',
    'Bury St Edmunds Regional Divorce Centre',
    'REGIONAL',
    ARRAY['Civil partnership'],
    '2nd Floor, Triton House, St Andrews Street North',
    'Bury St Edmunds',
    'Suffolk',
    'IP33 1TR',
    52.248581,
    0.711012
  ),
  (
    'civil-money-claims-service-centre',
    'Online Civil Money Claims Service Centre',
    'LOCAL',
    ARRAY['Money claims'],
    'HMCTS CMC, PO Box 12747',
    'Harlow',
    'Essex',
    'CM20 9RA',
    51.771744,
    0.093959
  ),
  (
    'civil-national-business-centre-cnbc',
    'Civil National Business Centre',
    'NATIONAL',
    ARRAY['Money claims'],
    'St Katharines House, 21-27 St Katharines Street',
    'Northampton',
    'Northamptonshire',
    'NN1 2LH',
    52.237302,
    -0.899965
  ),
  (
    'cleveland-durham-northumbria-and-north-yorkshire-central-enforcement-unit',
    'Cleveland Durham Northumbria and North Yorkshire Central Enforcement Unit',
    NULL,
    ARRAY[]::VARCHAR[],
    'PO Box 826',
    'North Shields',
    'Tyne and Wear',
    'NE29 1DZ',
    55.018204,
    -1.490330
  ),
  (
    'county-court-money-claims-centre-ccmcc',
    'County Court Money Claims Centre',
    'LOCAL',
    ARRAY['Money claims'],
    'County Court Money Claims Centre, PO Box 527',
    'Salford',
    'Greater Manchester',
    'M5 0BY',
    53.481200,
    -2.280431
  ),
  (
    'crime-service-centre',
    'Crime Service Centre',
    'NATIONAL',
    ARRAY['Other criminal offences'],
    'HMCTS Crime, PO Box 12888',
    'Harlow',
    'Essex',
    'CM20 9RW',
    51.771744,
    0.093959
  ),
  (
    'devon-and-cornwall-central-finance-unit',
    'Devon and Cornwall Central Finance Unit',
    NULL,
    ARRAY[]::VARCHAR[],
    'The Magistrates Court, St Andrew Street',
    'Plymouth',
    'Devon',
    'PL1 2DP',
    50.369287,
    -4.139575
  ),
  (
    'divorce-service-centre',
    'Divorce Service Centre',
    'NATIONAL',
    ARRAY['Civil partnership', 'Divorce'],
    'HMCTS Digital Divorce, PO Box 12706',
    'Harlow',
    'Essex',
    'CM20 9QT',
    51.771744,
    0.093959
  ),
  (
    'greater-manchester-central-accounts-and-enforcement-unit',
    'Greater Manchester Central Accounts and Enforcement Unit',
    NULL,
    ARRAY[]::VARCHAR[],
    'Manchester and Salford Magistrates Court, Crown Square',
    'Manchester',
    'Greater Manchester',
    'M60 1PR',
    53.487362,
    -2.227212
  ),
  (
    'harmondsworth-tribunal-hearing-centre',
    'Harmondsworth Tribunal Hearing Centre',
    'LOCAL',
    ARRAY['Immigration and asylum'],
    'Colnbrook Bypass',
    'Harmondsworth',
    'Greater London',
    'UB7 0HD',
    51.484605,
    -0.483988
  ),
  (
    'immigration-and-asylum-appeals-service-centre',
    'Immigration and Asylum Appeals Service Centre',
    'NATIONAL',
    ARRAY['Immigration and asylum'],
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  ),
  (
    'maintenance-enforcement-business-centre-england',
    'Maintenance Enforcement Business Centre England',
    NULL,
    ARRAY[]::VARCHAR[],
    'Triton House, St Andrews Street North',
    'Bury St Edmunds',
    'Suffolk',
    'IP33 1TR',
    52.248581,
    0.711012
  ),
  (
    'newport-south-wales-regional-divorce-centre',
    'Newport South Wales Regional Divorce Centre',
    'REGIONAL',
    ARRAY[]::VARCHAR[],
    '5th Floor, Clarence House, Clarence Place',
    'Newport',
    'Newport',
    'NP19 7AA',
    51.591016,
    -2.992500
  ),
  (
    'north-west-regional-divorce-centre',
    'North West Regional Divorce Centre',
    'REGIONAL',
    ARRAY[]::VARCHAR[],
    '35 Vernon Street',
    'Liverpool',
    'Merseyside',
    'L2 2BX',
    53.409485,
    -2.989390
  ),
  (
    'north-yorkshire-magistrates-courts-central-finance-unit',
    'North Yorkshire Magistrates Courts Central Finance Unit',
    NULL,
    ARRAY[]::VARCHAR[],
    'PO Box 826',
    'North Shields',
    'Tyne and Wear',
    'NE29 1DZ',
    55.018204,
    -1.490330
  ),
  (
    'pocock-street-tribunal-hearing-centre',
    'Pocock Street Tribunal Hearing Centre',
    'LOCAL',
    ARRAY['Money claims'],
    '18 Pocock Street',
    'London',
    'Greater London',
    'SE1 0BW',
    51.501876,
    -0.103489
  ),
  (
    'probate-service-centre',
    'Probate Service Centre',
    'NATIONAL',
    ARRAY['Probate'],
    'HMCTS Probate, PO Box 12625',
    'Harlow',
    'Essex',
    'CM20 9QF',
    51.771744,
    0.093959
  ),
  (
    'reading-county-court-and-family-court',
    'Reading County Court and Family Court',
    'LOCAL',
    ARRAY[
      'Adoption',
      'Bankruptcy',
      'Childcare arrangements if you separate from your partner',
      'Domestic abuse',
      'Female Genital Mutilation Protection Orders',
      'Forced marriage',
      'High Court district registry',
      'Housing',
      'Money claims'
    ],
    'Hearing Centre, 160-163 Friar Street',
    'Reading',
    'Berkshire',
    'RG1 1HE',
    51.456575,
    -0.971179
  ),
  (
    'reading-crown-court',
    'Reading Crown Court',
    'LOCAL',
    ARRAY[]::VARCHAR[],
    'Old Shire Hall, The Forbury',
    'Reading',
    'Berkshire',
    'RG1 3EH',
    51.456138,
    -0.967403
  ),
  (
    'reading-magistrates-court-and-family-court',
    'Reading Magistrates Court and Family Court',
    'LOCAL',
    ARRAY['Benefits'],
    'Castle Street',
    'Reading',
    'Berkshire',
    'RG1 7TQ',
    51.453342,
    -0.975742
  ),
  (
    'reading-tribunal-hearing-centre',
    'Reading Tribunal Hearing Centre',
    'LOCAL',
    ARRAY['Claims against employers'],
    '2nd Floor, 30-31 Friar Street',
    'Reading',
    'Berkshire',
    'RG1 1DX',
    51.456933,
    -0.974035
  ),
  (
    'single-justice-procedures-service-centre',
    'Single Justice Procedures Service Centre',
    'NATIONAL',
    ARRAY['Single Justice Procedure'],
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  ),
  (
    'social-security-and-child-support-appeals-service-centre',
    'SSCS Benefit Appeals Service Centre',
    'NATIONAL',
    ARRAY['Benefits'],
    'HMCTS SSCS Benefits Appeals, PO Box 12626',
    'Harlow',
    'Essex',
    'CM20 9QF',
    51.771744,
    0.093959
  ),
  (
    'south-west-regional-divorce-centre',
    'South West Regional Divorce Centre',
    'REGIONAL',
    ARRAY[]::VARCHAR[],
    'PO Box 1792',
    'Southampton',
    'Hampshire',
    'SO15 9GG',
    50.919240,
    -1.430790
  ),
  (
    'staffordshire-central-finance-and-enforcement-unit',
    'Staffordshire Central Finance and Enforcement Unit',
    NULL,
    ARRAY[]::VARCHAR[],
    'The Court House, Bryans Lane',
    'Rugeley',
    'Staffordshire',
    'WS15 2FX',
    52.761817,
    -1.932383
  );

UPDATE service_centre sc
SET name = source.name,
    open = TRUE,
    service_area_ids = CASE
      WHEN cardinality(source.service_area_names) = 0 THEN sc.service_area_ids
      ELSE ARRAY(
        SELECT sa.id
        FROM service_area sa
        WHERE sa.name = ANY(source.service_area_names)
        ORDER BY sa.name
      )
    END,
    catchment_type = COALESCE(source.catchment_type, sc.catchment_type),
    last_updated_at = CURRENT_TIMESTAMP
FROM seed_service_centre_location source
WHERE sc.slug = source.slug;

INSERT INTO service_centre (
  id,
  name,
  slug,
  open,
  created_at,
  last_updated_at,
  service_area_ids,
  catchment_type
)
SELECT
  gen_random_uuid(),
  source.name,
  source.slug,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  ARRAY(
    SELECT sa.id
    FROM service_area sa
    WHERE sa.name = ANY(source.service_area_names)
    ORDER BY sa.name
  ),
  source.catchment_type
FROM seed_service_centre_location source
WHERE NOT EXISTS (
  SELECT 1
  FROM service_centre sc
  WHERE sc.slug = source.slug
);

UPDATE service_centre_areas_of_law scaol
SET areas_of_law = source.area_of_law_ids
FROM (
  SELECT
    sc.id AS service_centre_id,
    ARRAY(
      SELECT DISTINCT sa.area_of_law_id
      FROM service_area sa
      WHERE sa.name = ANY(seed.service_area_names)
        AND sa.area_of_law_id IS NOT NULL
      ORDER BY sa.area_of_law_id
    ) AS area_of_law_ids
  FROM service_centre sc
  JOIN seed_service_centre_location seed ON seed.slug = sc.slug
) source
WHERE scaol.service_centre_id = source.service_centre_id
  AND cardinality(source.area_of_law_ids) > 0;

INSERT INTO service_centre_areas_of_law (
  id,
  service_centre_id,
  areas_of_law
)
SELECT
  gen_random_uuid(),
  source.service_centre_id,
  source.area_of_law_ids
FROM (
  SELECT
    sc.id AS service_centre_id,
    ARRAY(
      SELECT DISTINCT sa.area_of_law_id
      FROM service_area sa
      WHERE sa.name = ANY(seed.service_area_names)
        AND sa.area_of_law_id IS NOT NULL
      ORDER BY sa.area_of_law_id
    ) AS area_of_law_ids
  FROM service_centre sc
  JOIN seed_service_centre_location seed ON seed.slug = sc.slug
) source
WHERE cardinality(source.area_of_law_ids) > 0
  AND NOT EXISTS (
    SELECT 1
    FROM service_centre_areas_of_law scaol
    WHERE scaol.service_centre_id = source.service_centre_id
  );

UPDATE service_centre_address sca
SET address_line_1 = source.address_line_1,
    town_city = source.town_city,
    county = source.county,
    postcode = source.postcode,
    lat = source.lat,
    lon = source.lon
FROM service_centre sc
JOIN seed_service_centre_location source ON source.slug = sc.slug
WHERE sca.service_centre_id = sc.id
  AND sca.address_type = 'WRITE_TO_US'
  AND source.postcode IS NOT NULL
  AND source.lat IS NOT NULL
  AND source.lon IS NOT NULL;

INSERT INTO service_centre_address (
  id,
  service_centre_id,
  address_line_1,
  town_city,
  county,
  postcode,
  lat,
  lon,
  address_type
)
SELECT
  gen_random_uuid(),
  sc.id,
  source.address_line_1,
  source.town_city,
  source.county,
  source.postcode,
  source.lat,
  source.lon,
  'WRITE_TO_US'
FROM service_centre sc
JOIN seed_service_centre_location source ON source.slug = sc.slug
WHERE source.postcode IS NOT NULL
  AND source.lat IS NOT NULL
  AND source.lon IS NOT NULL
  AND NOT EXISTS (
  SELECT 1
  FROM service_centre_address sca
  WHERE sca.service_centre_id = sc.id
    AND sca.address_type = 'WRITE_TO_US'
);

COMMIT;

SELECT
  c.slug,
  ca.postcode,
  ca.lat,
  ca.lon
FROM court c
JOIN court_address ca ON ca.court_id = c.id
WHERE c.slug IN (
  'plymouth-combined-court',
  'plymouth-magistrates-court',
  'plymouth-social-security-and-child-support-tribunal',
  'bodmin-law-courts',
  'newton-abbot-magistrates-court',
  'torquay-and-newton-abbot-county-and-family-court',
  'truro-magistrates-court',
  'exeter-law-courts',
  'exeter-social-security-and-child-support-tribunal',
  'truro-combined-court',
  'barnstaple-magistrates-county-and-family-court',
  'taunton-crown-county-and-family-court',
  'taunton-magistrates-court-tribunals-and-family-hearing-centre',
  'yeovil-county-family-and-magistrates-court',
  'weymouth-combined-court',
  'port-talbot-justice-centre',
  'swansea-crown-court',
  'swansea-civil-justice-centre',
  'swansea-magistrates-court',
  'cardiff-magistrates-court',
  'wales-employment-tribunal',
  'llanelli-law-courts',
  'carmarthen-county-court-and-family-court',
  'cardiff-civil-and-family-justice-centre',
  'midlands-west-employment-tribunal',
  'taylor-house-tribunal-hearing-centre',
  'manchester-employment-tribunal',
  'edinburgh-upper-tribunal-administrative-appeals-chamber',
  'newport-south-wales-county-court-and-family-court',
  'bristol-civil-and-family-justice-centre',
  'bournemouth-combined-court',
  'southampton-combined-court-centre',
  'oxford-combined-court-centre',
  'birmingham-civil-and-family-justice-centre',
  'wrexham-county-and-family-court',
  'central-family-court',
  'portsmouth-combined-court-centre',
  'reading-county-court-and-family-court',
  'west-london-family-court',
  'brighton-hearing-centre',
  'luton-justice-centre',
  'pontypridd-county-court-and-family-court',
  'newport-south-wales-immigration-and-asylum-tribunal',
  'birmingham-immigration-and-asylum-chamber-first-tier-tribunal',
  'harmondsworth-tribunal-hearing-centre',
  'hatton-cross-tribunal-hearing-centre',
  'coventry-magistrates-court',
  'field-house-tribunal-hearing-centre',
  'yarls-wood-immigration-and-asylum-hearing-centre'
)
AND ca.address_type = 'VISIT_US'
ORDER BY point(ca.lon, ca.lat) <@> point(-4.225, 50.408);

SELECT
  (SELECT COUNT(*) FROM service_centre) AS service_centres,
  (
    SELECT COUNT(DISTINCT service_centre_id)
    FROM service_centre_address
    WHERE address_type = 'WRITE_TO_US'
      AND postcode IS NOT NULL
      AND lat IS NOT NULL
      AND lon IS NOT NULL
  ) AS geocoded_service_centres,
  (SELECT COUNT(*) FROM court_single_points_of_entry) AS court_spoe_rows,
  (SELECT COUNT(*) FROM court_local_authorities) AS court_local_authority_rows;

SELECT sc.slug AS service_centre_without_source_location
FROM service_centre sc
WHERE sc.slug IN (
  'immigration-and-asylum-appeals-service-centre',
  'single-justice-procedures-service-centre'
)
AND NOT EXISTS (
  SELECT 1
  FROM service_centre_address sca
  WHERE sca.service_centre_id = sc.id
    AND sca.address_type = 'WRITE_TO_US'
    AND sca.postcode IS NOT NULL
    AND sca.lat IS NOT NULL
    AND sca.lon IS NOT NULL
)
ORDER BY sc.slug;

SELECT
  sc.name,
  sc.slug,
  sc.catchment_type,
  sca.address_type,
  sca.postcode
FROM service_centre sc
JOIN service_centre_address sca ON sca.service_centre_id = sc.id
JOIN service_centre_areas_of_law scaol ON scaol.service_centre_id = sc.id
JOIN service_area sa ON sa.id = ANY(sc.service_area_ids)
WHERE sc.open = TRUE
  AND sa.name = 'Civil partnership'
  AND sa.area_of_law_id = ANY(scaol.areas_of_law)
  AND sc.catchment_type IN ('LOCAL', 'REGIONAL')
  AND sca.address_type = 'WRITE_TO_US'
ORDER BY sc.name;

SELECT
  sc.name,
  sc.slug,
  sc.catchment_type,
  sca.address_type,
  sca.postcode
FROM service_centre sc
JOIN service_centre_address sca ON sca.service_centre_id = sc.id
JOIN service_area sa ON sa.id = ANY(sc.service_area_ids)
WHERE sc.open = TRUE
  AND sa.name = 'Other criminal offences'
  AND sc.catchment_type = 'NATIONAL'
  AND sca.address_type = 'WRITE_TO_US'
ORDER BY sc.name;

SELECT c.slug AS cornwall_children_spoe
FROM court c
JOIN court_single_points_of_entry spoe ON spoe.court_id = c.id
JOIN area_of_law_types aol ON aol.id = ANY(spoe.areas_of_law)
JOIN court_local_authorities cla
  ON cla.court_id = c.id
  AND cla.area_of_law_id = aol.id
JOIN local_authority_types lat ON lat.id = ANY(cla.local_authority_ids)
WHERE aol.name = 'Children'
  AND lat.name = 'Cornwall Council';

WITH money_claims AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Money claims'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN money_claims aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH immigration AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Immigration'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN immigration aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH adoption AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Adoption'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN adoption aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH forced_marriage AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Forced marriage'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN forced_marriage aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH financial_remedy AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Financial Remedy'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN financial_remedy aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH tax AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Tax'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN tax aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH employment AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Employment'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN employment aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH benefits AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Social security'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN benefits aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;

WITH bankruptcy AS (
  SELECT id
  FROM area_of_law_types
  WHERE name = 'Bankruptcy'
)
SELECT
  c.name,
  c.slug,
  ROUND(
    (point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
      <@> point(-4.225, 50.408))::NUMERIC,
    1
  ) AS approximate_miles_from_pl12_4er
FROM court c
JOIN court_address ca ON ca.court_id = c.id
JOIN court_areas_of_law coa ON coa.court_id = c.id
JOIN bankruptcy aol ON aol.id = ANY(coa.areas_of_law)
WHERE c.open = TRUE
  AND ca.address_type = 'VISIT_US'
  AND ca.lat IS NOT NULL
  AND ca.lon IS NOT NULL
ORDER BY point(CAST(ca.lon AS FLOAT8), CAST(ca.lat AS FLOAT8))
  <@> point(-4.225, 50.408)
LIMIT 10;
