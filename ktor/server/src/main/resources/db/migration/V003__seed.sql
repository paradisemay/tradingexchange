INSERT INTO instruments (ticker, name, currency, lot_size, is_active, last_price)
VALUES
    ('SBER',  'Сбербанк',           'RUB', 10,  TRUE, 252.0000),
    ('GAZP',  'Газпром',            'RUB', 10,  TRUE, 165.0000),
    ('YNDX',  'Яндекс',             'RUB', 1,   TRUE, 4100.0000),
    ('LKOH',  'Лукойл',             'RUB', 1,   TRUE, 7200.0000),
    ('GMKN',  'Норникель',          'RUB', 1,   TRUE, 14500.0000)
ON CONFLICT (ticker) DO NOTHING;
