UPDATE area_of_law_types aolt
SET name_cy = v.name_cy
FROM (
    VALUES
        ('Divorce', 'Ysgariad'),
        ('Civil partnership', 'Partneriaeth sifil'),
        ('High Court District Registry', 'Cofrestrfa Ddosbarth yr Uchel Lys'),
        ('Adoption', 'Mabwysiadu'),
        ('Social security', 'Nawdd Cymdeithasol'),
        ('Housing possession', 'Meddiannu eiddo'),
        ('Money claims', 'Hawliadau am arian'),
        ('Employment', 'Cyflogaeth'),
        ('Tax', 'Treth'),
        ('Children', 'Plant'),
        ('Domestic violence', 'Trais domestig'),
        ('Crime', 'Trosedd'),
        ('Civil', 'Sifil'),
        ('Single justice procedure', 'Y Weithdrefn Un Ynad'),
        ('High Court', 'Yr Uchel Lys'),
        ('Court Of Appeal', 'Y Llys Apêl'),
        ('Financial Remedy', 'Rhwymedi Ariannol'),
        ('Business and Property', 'Busnes ac Eiddo'),
        ('Bankruptcy', 'Methdaliad'),
        ('Immigration', 'Mewnfudo'),
        ('Probate', 'Profiant'),
        ('Forced marriage', 'Priodas dan orfod'),
        ('FGM', 'Anffurfio Organau Cenhedlu Benywod (FGM)'),
        ('Pathfinder – for Private Law Proceedings', 'Cynllun Braenaru – ar gyfer Achosion Cyfraith Breifat'),
        ('Domestic Abuse Protection Order (DAPOs)', 'Gorchymyn Diogelu Rhag Cam-drin Domestig (DAPOs)'),
        ('Benefits', 'Budd-daliadau')
) AS v(name, name_cy)
WHERE aolt.name = v.name;

