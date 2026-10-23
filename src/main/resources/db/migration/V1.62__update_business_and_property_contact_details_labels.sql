-- FACT-3178: Update contact description types for Business and Property Court
--            to Business and Property Division.
UPDATE contact_description_types cdt
SET name = v.name_new,
    name_cy = v.name_cy_new
  FROM (
    VALUES
        ('Business and Property Court appointments', 'Business and Property Division appointments', 'Penodiadau’r Adran Fusnes ac Eiddo'),
        ('Business and Property Court enquiries', 'Business and Property Division enquiries', 'Ymholiadau’r Adran Fusnes ac Eiddo'),
        ('Business and Property Court lists', 'Business and Property Division Lists', 'Rhestrau’r Adran Fusnes ac Eiddo')
) AS v(name, name_new, name_cy_new)
WHERE cdt.name = v.name;
