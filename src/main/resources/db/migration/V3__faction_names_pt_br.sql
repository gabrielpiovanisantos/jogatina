-- Facções passam a usar os nomes do manual brasileiro "As Leis de Root".
-- O finish grava a facção como foi digitada, então a comparação ignora
-- maiúsculas, minúsculas e espaços nas pontas. Valores fora da lista ficam como estão.
update game_results r
set faction = m.pt_br
from (values
    ('marquise de cat',      'Marqueses'),
    ('eyrie dynasties',      'Dinastia das Rapinas'),
    ('woodland alliance',    'Aliança da Floresta'),
    ('vagabond',             'Malandro'),
    ('lizard cult',          'Lagartos Cultistas'),
    ('riverfolk company',    'Companhia Ribeirinha'),
    ('underground duchy',    'Ducado Subterrâneo'),
    ('corvid conspiracy',    'Conspiração Corvídea'),
    ('lord of the hundreds', 'Senhor das Centenas'),
    ('keepers in iron',      'Guardiões de Ferro')
) as m(en, pt_br)
where lower(btrim(r.faction, E' \t\r\n')) = m.en;
